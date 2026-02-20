package nnsql.query.renderer;

import nnsql.query.ir.*;

public class PlainIRRenderer implements IRRenderer {

    @Override
    public String render(IRNode ir) {
        return renderNode(ir, 0);
    }

    private String renderNode(IRNode node, int depth) {
        var indent = "  ".repeat(depth);
        var nodeStr = new StringBuilder(indent + node.toString());

        var inputNode = switch (node) {
            case Product _ -> null;
            case Filter f -> f.input();
            case Group g -> g.input();
            case AggFilter af -> af.input();
            case Return r -> r.input();
            case DuplElim d -> d.input();
        };

        if (inputNode != null) {
            nodeStr.append("\n").append(renderNode(inputNode, depth + 1));
        }

        return nodeStr.toString();
    }
}
