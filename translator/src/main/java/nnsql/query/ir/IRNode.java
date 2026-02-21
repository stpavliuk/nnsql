package nnsql.query.ir;

public sealed interface IRNode
    permits Product, Filter, Group, AggFilter, Return, DuplElim, Sort {

    String toString();
}
