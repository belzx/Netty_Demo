package com.lizhi.rpcdemo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@ToString
@Getter
@Setter
public class ClassInfo implements Serializable {
  
    private static final long serialVersionUID = -8970942815543515064L;  
    private String className;//类名
    private String methodName;//函数名称  
    private Class<?>[] types;//参数类型    
    private Object[] objects;//参数列表    
}
