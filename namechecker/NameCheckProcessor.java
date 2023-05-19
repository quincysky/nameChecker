package jvm.namechecker;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * @author quincy
 * @create 2023 - 05 - 19 13:01
 */
// 表示对所有的注解都感兴趣
@SupportedAnnotationTypes("*")
// 只支持Java8的代码
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class NameCheckProcessor extends AbstractProcessor {
    private NameChecker nameChecker;

    /**
     * 初始化名称检查插件
     * @param processingEnv
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        nameChecker = new NameChecker(processingEnv);
    }

    /**
     * 对输入的语法树的各个节点进行名称检查
     * @param annotations
     * @param roundEnv
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(!roundEnv.processingOver()) {
            for (Element element : roundEnv.getRootElements())
                nameChecker.checkName(element);
        }
        return false;
    }
}
