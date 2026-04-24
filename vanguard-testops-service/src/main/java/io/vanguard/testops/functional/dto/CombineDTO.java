package io.vanguard.testops.functional.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author Jan
 */
@Data
public class CombineDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private List<Object> value;
    private String type;
    private String operator;

}
