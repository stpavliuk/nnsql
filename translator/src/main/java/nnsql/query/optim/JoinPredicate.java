package nnsql.query.optim;

public record JoinPredicate(
    int leftRelIndex,
    String leftAttr,
    int rightRelIndex,
    String rightAttr
) {
}
