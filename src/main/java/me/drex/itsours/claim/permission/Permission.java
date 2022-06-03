package me.drex.itsours.claim.permission;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.itsours.claim.permission.node.Node;

import java.util.List;

public interface Permission {

    boolean includes(Permission other);

    void validateContext(Node.ChangeContext context) throws CommandSyntaxException;

    List<Node> getNodes();

    String asString();

}
