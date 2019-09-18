package com.lizhi.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;

import java.util.Arrays;

public class TestDemo {

    @Test
    public void test(){
        byte[] bytes = enSer();
        System.out.println(bytes.length);
        System.out.println(Arrays.toString(bytes));

        try {
            TeacherSerializer.Teacher teacher = deSer(bytes);
            System.out.println(teacher.getTeacherId());
            System.out.println(teacher.getAge());
            System.out.println(teacher);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

    }

    //序列化
    public static byte[] enSer(){
        TeacherSerializer.Teacher.Builder builder = TeacherSerializer.Teacher.newBuilder();
        builder.setTeacherId(1).setAge(32).setName("nizi").addCourses("java");
        TeacherSerializer.Teacher build = builder.build();
        return build.toByteArray();
    }

    //反序列化
    public static TeacherSerializer.Teacher deSer(byte[] bytes) throws InvalidProtocolBufferException {
        TeacherSerializer.Teacher teacher = TeacherSerializer.Teacher.parseFrom(bytes);
        return teacher;
    }
}
