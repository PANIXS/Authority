package com.mmall.beans;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mail {

    private String subject;

    private String message;

    private Set<String> receivers;

}
