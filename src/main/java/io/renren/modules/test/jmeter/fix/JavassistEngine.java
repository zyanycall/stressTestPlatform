package io.renren.modules.test.jmeter.fix;

import io.renren.common.exception.RRException;
import org.apache.ibatis.javassist.*;

/**
 * 字节码修改的技术，改变StandardJMeterEngine的字节码，相当于修改源码。
 * 现在测试还没有完全通过，等稳定后再使用，替代LocalStandardJmeterEngine
 * Created by zyanycall@gmail.com on 2018/11/6 17:29.
 */
public class JavassistEngine {

    public static Class<?> engineClazz;

    public void fixJmeterStandrdEngine() {
        //开始获取class的文件
        try {
            String classFileName = "org.apache.jmeter.engine.StandardJMeterEngine";

            ClassPool pool = ClassPool.getDefault();
            pool.insertClassPath(new ClassClassPath(this.getClass()));
            CtClass cc = pool.get(classFileName);

            // 添加引用类
            // 本身是pool编译代码是需要引用的。即修改类，添加方法等都会使用，即对下面的字节码修改都是有效的。
            // 这里引用之后，下面body体等就不用写全路径了。
            pool.importPackage("io.renren.modules.test.entity.StressTestFileEntity");
            pool.importPackage("io.renren.modules.test.jmeter.JmeterTestPlan");
            pool.importPackage("org.apache.jorphan.collections.SearchByClass");
            pool.importPackage("org.apache.jorphan.collections.HashTree");

            // 向CtClass中添加一个字段
            CtField cf = CtField.make("private StressTestFileEntity stressTestFile;", cc);
            // 添加字段
            cc.addField(cf);

            // 添加新的字段的set方法
            CtMethod m = CtNewMethod.make(
                    "public void setStressTestFile(StressTestFileEntity stressTestFile) { this.stressTestFile = stressTestFile; }",
                    cc);
            cc.addMethod(m);

            // 核心，修改configure方法
            CtMethod configure = cc.getDeclaredMethod("configure");
            // 设置新名称，其实就是删除原方法
            configure.setName(configure.getName() + "_orig");

            // 添加新方法，即替换掉原来的confiure
            // javassist 不支持泛型，需要去掉<>。
            // 同时为了避免麻烦，将类全名写出。
            CtMethod configureNew = CtNewMethod.make(
                    "public void configure(HashTree testTree) {\n" +
                            "        SearchByClass jmeterTestPlan = new SearchByClass(JmeterTestPlan.class);\n" +
                            "        JmeterTestPlan tpTemp = new JmeterTestPlan();\n" +
                            // testPlan对应的是测试计划，每一个测试脚本之中只有一个测试计划，所以直接取第一个即可。
                            // 交换key值，让我们的自实现的子类jmeterTestPlan进入。
                            "        testTree.replaceKey(testTree.keySet().toArray()[0], tpTemp);\n" +
                            "        testTree.traverse(jmeterTestPlan);\n" +
                            "        Object[] plan = jmeterTestPlan.getSearchResults().toArray();\n" +
                            "        if (plan.length == 0) {\n" +
                            "            throw new RuntimeException(\"Could not find the TestPlan class!\");\n" +
                            "        }\n" +
                            "        JmeterTestPlan tp = (JmeterTestPlan) plan[0];\n" +
                            // 设置我们平台自己的变量
                            "        tp.setStressTestFile(stressTestFile);\n" +
                            "        serialized = tp.isSerialized();\n" +
                            "        tearDownOnShutdown = tp.isTearDownOnShutdown();\n" +
                            "        active = true;\n" +
                            "        test = testTree;\n" +
                            "    }",
                    cc);
            cc.addMethod(configureNew);

//            CtClass ccStopTest = pool.get("org.apache.jmeter.engine.StandardJMeterEngine$StopTest");
//            ccStopTest.setName(ccStopTest.getName() + "_orig");
            // 向CtClass中添加一个字段
//            CtField cfStopTest = CtField.make("private Integer aaaa;", ccStopTest);
//            // 添加字段
//            ccStopTest.addField(cfStopTest);

            engineClazz = cc.toClass();
        } catch (NotFoundException | CannotCompileException e) { //类文件没有找到   无法编译
            throw new RRException(e.getMessage(), e);
        }
    }
}
