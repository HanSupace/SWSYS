package com.daily.lastsys.common;

import java.util.List;

public record ValidationErrorResponse(List<String> errors) {
}
