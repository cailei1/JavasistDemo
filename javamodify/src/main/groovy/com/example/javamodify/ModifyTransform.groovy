package com.example.javamodify

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.gradle.api.Project

class ModifyTransform extends Transform{

    def project

    //字节码池
    def pool= ClassPool.default

    ModifyTransform(Project project){
        this.project=project
    }

    @Override
    String getName() {
        //我家
        return "cailei"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        //接收的输入文件
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        //作用域:整个工程
        return TransformManager.SCOPE_FULL_PROJECT
    }


    //增量编译 修改就编译不修改就不编译
    @Override
    boolean isIncremental() {
        return false
    }


    //从上一个接盘侠手中 拿到东西 处理 然后发送给下一个接盘侠
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        //遍历输入的东西
        transformInvocation.inputs.each {
           it.jarInputs.each {
               //这个jar包直接丢给下一个
               pool.insertClassPath(it.file.absolutePath)
               def dest=transformInvocation.outputProvider.getContentLocation(it.name,it.contentTypes,it.scopes,Format.JAR)
               FileUtils.copyFile(it.file,dest)
           };

            it.directoryInputs.each {
                //处理
                def preFileName=it.file.absolutePath
                //把类加载到PC 内存
                pool.insertClassPath(preFileName)
                findTarget(it.file,preFileName)

                def dest=transformInvocation.outputProvider.getContentLocation(it.name,it.contentTypes,it.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(it.file,dest)
            }


        }

    }

    private void findTarget(File dir,String fileName){
        if(dir.isDirectory()){
            dir.listFiles().each {
                findTarget(it,fileName)
            }
        }else{
            //不是文件就去修改这个class
            modify(dir,fileName)
        }
    }

    private void modify(File dir,String fileName){
         def filepath=dir.absolutePath
         if(!filepath.endsWith(".class")){
             return
         }
        if(filepath.endsWith('R$')||filepath.endsWith('R.class')||filepath.endsWith('BuildConfig.class')){
            return
        }
      //全类名
       def classNmae= filepath.replace(fileName,"").replace("\\",".")
               .replace("/",".")
       def name=classNmae.replace(".class","").substring(1)
        if(name.contains("com.example.javasistdemo")){
          CtClass ctClass= pool.get(name)
            def body="int i=0;"
            addCode(ctClass,body,fileName)
        }

    }

    private void addCode(CtClass ctClass, String body, String fileName){
//        if(ctClass.getName().contains("PatchProxy")){
//            return;
//        }
        CtMethod[] methods=ctClass.getDeclaredMethods()
        for(method in methods){
            System.out.printf(method.name)
            if(method.name.contains("onCreate")){
            method.insertBefore(body)
            }
        }
        ctClass.writeFile(fileName)
        ctClass.detach()
    }

}