package com.worklink.service.semantic;

/** Text from one persisted profile field and its relative term-frequency weight. */
public record SemanticTextField(String text, double weight) {
}
