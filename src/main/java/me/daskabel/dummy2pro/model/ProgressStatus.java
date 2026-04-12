package me.daskabel.dummy2pro.model;

/**
 * Legt die möglichen Bearbeitungsstände einer Frage fest.
 *
 * OPEN steht für noch unbeantwortet, CORRECT für richtig beantwortet
 * und WRONG für falsch beantwortet.
 */
public enum ProgressStatus
{
    OPEN,
    CORRECT,
    WRONG
}
