package com.example.javamodify

import org.gradle.api.Plugin
import org.gradle.api.Project


class ModifyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        //可以理解为main函数 编译链的入口 将transform 注入到编译链中
      project.android.registerTransform(new ModifyTransform(project))
    }
}