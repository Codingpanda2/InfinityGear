package com.infinitygear.api;

public record OperationResult<T>(boolean success, FailureReason reason, String messageKey, T value) {
    public static <T> OperationResult<T> success(T value) {
        return new OperationResult<>(true, FailureReason.NONE, "", value);
    }
    public static <T> OperationResult<T> failure(FailureReason reason, String messageKey) {
        return new OperationResult<>(false, reason, messageKey, null);
    }
}
