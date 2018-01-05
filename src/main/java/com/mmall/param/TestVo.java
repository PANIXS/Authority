package com.mmall.param;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class TestVo {
    @NotBlank(message = "msg:人家不能为空")
    private String msg;
    @NotNull(message = "id不可以为空")
    @Max(value = 10,message = "id不能大于10")
    @Min(value = 0,message = "id姚道玉扥估10")
    private Integer id;
    private List<String> str;
}
