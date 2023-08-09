package org.mifos.processor.bulk.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Program {

    private String id;
    private String name;
    private String identifierType;
    private String identifierValue;

}
