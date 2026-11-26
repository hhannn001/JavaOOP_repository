package coderoom.features.gameplay;

import coderoom.shared.ui.SpecimenMark;

/** The mark conditions shared by the specimen note, diagram and grader. */
public record SpecimenRule(SpecimenMark candidateLeaf, SpecimenMark candidateStamp,
                           SpecimenMark excludedLeaf, SpecimenMark excludedStamp) {
    boolean keeps(SpecimenMark[] marks) {
        return (marks[0] == candidateLeaf || marks[2] == candidateStamp)
                && marks[1] != SpecimenMark.DAMAGED
                && !(marks[0] == excludedLeaf && marks[2] == excludedStamp);
    }

    public String javaConditions() {
        return """
                boolean validShape =
                    leaf == '%c' || stamp == '%c';
                boolean reject =
                    state == '%c'
                    || (leaf == '%c'
                        && stamp == '%c');""".formatted(candidateLeaf.symbol(), candidateStamp.symbol(),
                SpecimenMark.DAMAGED.symbol(), excludedLeaf.symbol(), excludedStamp.symbol());
    }
}
