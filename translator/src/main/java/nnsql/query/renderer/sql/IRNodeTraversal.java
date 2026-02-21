package nnsql.query.renderer.sql;

import nnsql.query.ir.AggFilter;
import nnsql.query.ir.DuplElim;
import nnsql.query.ir.Filter;
import nnsql.query.ir.Group;
import nnsql.query.ir.IRNode;
import nnsql.query.ir.Product;
import nnsql.query.ir.Return;
import nnsql.query.ir.Sort;

final class IRNodeTraversal {

    private IRNodeTraversal() {
        throw new UnsupportedOperationException("Utility class");
    }

    static Return findReturnNode(IRNode node) {
        return switch (node) {
            case Return r -> r;
            case Sort s -> findReturnNode(s.input());
            case DuplElim d -> findReturnNode(d.input());
            case AggFilter af -> findReturnNode(af.input());
            case Group g -> findReturnNode(g.input());
            case Filter f -> findReturnNode(f.input());
            case Product _ -> null;
        };
    }
}
