package nnsql.query.renderer;

import nnsql.query.ir.IRNode;

public interface IRRenderer {
    String render(IRNode ir);
}
