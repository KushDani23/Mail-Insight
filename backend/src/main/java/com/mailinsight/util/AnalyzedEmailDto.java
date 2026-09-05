package com.mailinsight.util;

import com.mailinsight.enums.EmailCategory;
import com.mailinsight.enums.EmailPriority;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalyzedEmailDto {

    private final String gmailMessageId;
    private final String summary;
    private final EmailCategory category;
    private final EmailPriority priority;
}
