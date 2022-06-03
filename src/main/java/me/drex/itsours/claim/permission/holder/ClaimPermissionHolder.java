package me.drex.itsours.claim.permission.holder;

import me.drex.itsours.claim.AbstractClaim;
import me.drex.itsours.claim.permission.Permission;
import me.drex.itsours.claim.permission.context.GlobalContext;
import me.drex.itsours.claim.permission.context.PersonalContext;
import me.drex.itsours.claim.permission.context.RoleContext;
import me.drex.itsours.claim.permission.roles.Role;
import me.drex.itsours.claim.permission.roles.RoleManager;
import me.drex.itsours.claim.permission.util.Value;
import me.drex.itsours.claim.permission.visitor.PermissionVisitor;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClaimPermissionHolder {

    public PermissionHolder settings = PermissionHolder.storage();
    public Map<UUID, PermissionHolder> playerPermission = new HashMap<>();
    public Map<UUID, PlayerRoleHolder> roles = new HashMap<>();

    public ClaimPermissionHolder(NbtCompound tag) {
        fromNBT(tag);
    }

    private void fromNBT(NbtCompound tag) {
        if (tag.contains("settings")) settings.load(tag.getCompound("settings"));
        if (tag.contains("players")) {
            NbtCompound players = tag.getCompound("players");
            players.getKeys().forEach(uuid -> {
                NbtCompound player = players.getCompound(uuid);
                if (player.contains("permission"))
                    playerPermission.put(UUID.fromString(uuid), PermissionHolder.fromNbt(player.getCompound("permission")));
                if (player.contains("role")) {
                    roles.put(UUID.fromString(uuid), new PlayerRoleHolder(player.get("role")));
                }
            });
        }
    }

    public NbtCompound toNBT() {
        NbtCompound tag = new NbtCompound();
        NbtCompound players = new NbtCompound();

        List<UUID> uuidSet = new ArrayList<>();
        uuidSet.addAll(playerPermission.keySet());
        uuidSet.addAll(roles.keySet());
        for (UUID uuid : uuidSet) {
            boolean shouldSave = false;
            NbtCompound player = new NbtCompound();
            PermissionHolder storage = playerPermission.get(uuid);
            if (storage != null) {
                player.put("permission", storage.save());
                if (storage.size() > 0) shouldSave = true;
            }
            PlayerRoleHolder roleStorage = roles.get(uuid);
            if (roleStorage != null) {
                player.put("role", roleStorage.toNbt());
                if (roleStorage.getSize() > 0) shouldSave = true;
            }
            if (shouldSave) players.put(uuid.toString(), player);
        }

        if (!settings.isEmpty()) tag.put("settings", settings.save());
        tag.put("players", players);
        return tag;
    }

    public boolean addRole(UUID uuid, Role role) {
        PlayerRoleHolder roleStorage = this.roles.computeIfAbsent(uuid, ignored -> new PlayerRoleHolder());
        return roleStorage.addRole(role);
    }

    public boolean removeRole(UUID uuid, Role role) {
        PlayerRoleHolder roleStorage = this.roles.computeIfAbsent(uuid, ignored -> new PlayerRoleHolder());
        return roleStorage.removeRole(role);
    }

    public List<Role> getRoles(UUID uuid) {
        PlayerRoleHolder roleStorage = this.roles.get(uuid);
        if (roleStorage != null) {
            return roleStorage.getRoles();
        }
        return Collections.emptyList();
    }

    public Map<UUID, PlayerRoleHolder> getRoles() {
        return roles;
    }

    public void visit(AbstractClaim claim, @Nullable UUID uuid, Permission permission, PermissionVisitor visitor) {
        settings.visit(claim, permission, GlobalContext.INSTANCE, visitor);
        if (uuid != null) {
            PermissionHolder permissionStorage = playerPermission.get(uuid);
            if (permissionStorage != null) {
                permissionStorage.visit(claim, permission, new PersonalContext(uuid), visitor);
            }
            PlayerRoleHolder roleStorage = roles.get(uuid);
            if (roleStorage != null) {
                Role defaultRole = RoleManager.INSTANCE.getRole(RoleManager.DEFAULT_ID);
                assert defaultRole != null;
                defaultRole.permissions().visit(claim, permission, new RoleContext(defaultRole), visitor);
                for (Role role : roleStorage.getRoles()) {
                    role.permissions().visit(claim, permission, new RoleContext(role), visitor);
                }
            }
        }
    }

    public void setPermission(UUID uuid, Permission permission, Value value) {
        PermissionHolder storage = playerPermission.computeIfAbsent(uuid, ignored -> PermissionHolder.storage());
        storage.set(permission, value);
    }

    public Value getPermission(UUID uuid, Permission permission) {
        return getPermission(uuid).get(permission);
    }

    public PermissionHolder getPermission(UUID uuid) {
        return playerPermission.computeIfAbsent(uuid, ignored -> PermissionHolder.storage());
    }

}
