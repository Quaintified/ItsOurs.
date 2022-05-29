package me.drex.itsours.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.itsours.claim.AbstractClaim;
import me.drex.itsours.claim.permission.Permission;
import me.drex.itsours.claim.permission.util.Value;
import me.drex.itsours.claim.permission.context.GlobalContext;
import me.drex.itsours.claim.permission.node.Node;
import me.drex.itsours.command.argument.ClaimArgument;
import me.drex.itsours.command.argument.PermissionArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class GlobalSettingCommand extends AbstractCommand {

    public static final GlobalSettingCommand INSTANCE = new GlobalSettingCommand();

    public GlobalSettingCommand() {
        super("globalSetting");
    }

    @Override
    protected void register(LiteralArgumentBuilder<ServerCommandSource> literal) {
        literal.then(
                ClaimArgument.ownClaims()
                        .then(
                                literal("check").then(
                                        PermissionArgument.permission()
                                                .executes(ctx -> executeCheck(ctx.getSource(), ClaimArgument.getClaim(ctx), PermissionArgument.getPermission(ctx)))
                                )
                        ).then(
                                literal("set").then(
                                        PermissionArgument.permission().then(
                                                PermissionArgument.value()
                                                        .executes(ctx -> executeSet(ctx.getSource(), ClaimArgument.getClaim(ctx), PermissionArgument.getPermission(ctx), PermissionArgument.getValue(ctx)))
                                        )
                                )
                        ).then(
                                literal("unset").then(
                                        PermissionArgument.permission()
                                                .executes(ctx -> executeSet(ctx.getSource(), ClaimArgument.getClaim(ctx), PermissionArgument.getPermission(ctx), Value.UNSET))
                                )
                        )
        );
    }

    public int executeSet(ServerCommandSource src, AbstractClaim claim, Permission permission, Value value) throws CommandSyntaxException {
        // TODO: Check if executor has permission to change permission
        for (Node node : permission.getNodes()) {
            // TODO: Add to other contexts
            if (!node.canChange(new Node.ChangeContext(claim, GlobalContext.INSTANCE, value, src))) throw PermissionArgument.FORBIDDEN;
        }
        claim.getPermissionManager().settings.set(permission, value);
        src.sendFeedback(Text.translatable("text.itsours.commands.globalSetting.set",
                permission.asString(), claim.getFullName(), value.format()
        ), false);
        return 1;
    }

    public int executeCheck(ServerCommandSource src, AbstractClaim claim, Permission permission) {
        // TODO: Check if executor has permission to check permission
        Value value = claim.getPermissionManager().settings.get(permission);
        src.sendFeedback(Text.translatable("text.itsours.commands.globalSetting.check", permission.asString(), claim.getFullName(), value.format()), false);
        return 1;
    }

}