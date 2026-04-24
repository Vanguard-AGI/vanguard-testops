package io.vanguard.testops.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Jan
 */
@Data
public class BugRelateCaseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    private String title;

    private String status;

}
