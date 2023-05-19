package jvm.namechecker;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementScanner8;
import javax.tools.Diagnostic;
import java.util.EnumSet;

import static javax.lang.model.element.Modifier.*;

/**
 * 程序名称规范的编译插件<br/>
 * 如果程序命名不符合规范，将会输出一个编译器的WARNING信息
 *
 * @author quincy
 * @create 2023 - 05 - 19 13:03
 */
public class NameChecker {

    private final Messager messager;

    NameCheckerScanner nameCheckerScanner = new NameCheckerScanner();
    NameChecker(ProcessingEnvironment processingEnvironment) {
        this.messager = processingEnvironment.getMessager();
    }

    /**
     * <ul>
     *     <li>类或接口：驼峰式命名，首字母大写</li>
     *     <li>方法：符合驼峰式命名，首字母小写</li>
     *     <li>字段： 类、实例变量：符合驼峰式命名法，首字母小写。 常量： 要求全部大写</li>
     * </ul>
     * @param element
     */
    public void checkName(Element element) {
        nameCheckerScanner.scan(element);
    }

    private  class NameCheckerScanner extends ElementScanner8<Void, Void> {


        /**
         * 此方法用于检查Java类
         * @param e
         * @param unused
         * @return
         */
        @Override
        public Void visitType(TypeElement e, Void unused) {
            scan(e.getTypeParameters(), unused);
            checkCamelCase(e, true);
            super.visitType(e, unused);
            return null;
        }

        /**
         * 检查方法命名是否合法
         * @param e
         * @param unused
         * @return
         */
        @Override
        public Void visitExecutable(ExecutableElement e, Void unused) {
            if (e.getKind() == ElementKind.METHOD) {
                Name name = e.getSimpleName();
                if (name.contentEquals(e.getEnclosingElement().getSimpleName()))
                    messager.printMessage(Diagnostic.Kind.WARNING, "一个普通方法 " + name + "不应该与类名重复，避免和构造函数产生混淆", e);
                checkCamelCase(e, false);
            }
            super.visitExecutable(e, unused);
            return null;
        }

        /**
         * 检查变量命名是否合法
         * @param e
         * @param unused
         * @return
         */
        @Override
        public Void visitVariable(VariableElement e, Void unused) {
            // 如果这个变量是枚举或常量，则按大写命名检查，否则按照驼峰式命名法规则检查
            if (e.getKind() == ElementKind.ENUM_CONSTANT || e.getConstantValue() != null || heuristicallyConstant(e)) {
                checkAllCaps(e);
            } else {
                checkCamelCase(e, false);
            }
            return null;
        }

        /**
         * 判断一个变量是否是常量
         * @param e
         * @return
         */
        private boolean heuristicallyConstant(VariableElement e) {
            if (e.getEnclosingElement().getKind() == ElementKind.INTERFACE) {
                return true;
            }
            else if (e.getKind() == ElementKind.FIELD && e.getModifiers().containsAll(EnumSet.of (PUBLIC, STATIC, FINAL)))
                return true;
            else
                return false;
        }

        /**
         * 检查传入的Element是否符合驼式命名法，如果不符合，则输出警告信息
         * @param e
         * @param initialCaps
         */
        private void checkCamelCase(Element e, boolean initialCaps) {
            String name = e.getSimpleName().toString();
            boolean previousUpper = false;
            boolean conventional = true;

            // 返回指定索引处的字符
            int firstCodePoint = name.codePointAt(0);
            if (Character.isUpperCase(firstCodePoint)) {
                previousUpper = true;
                if (!initialCaps) {
                    messager.printMessage(Diagnostic.Kind.WARNING, "名称 " + name + "应当以小写字母开头", e);
                    return;
                }
            } else if (Character.isLowerCase(firstCodePoint)) {
                if (initialCaps) {
                    messager.printMessage(Diagnostic.Kind.WARNING, "名称 " + name + "应当以大写字母开头", e);
                    return;
                }
            } else {
                conventional = false;
            }

            if (conventional) {
                int cp = firstCodePoint;
                // charCount(int codePoint)表示返回指定字符所需的char值
                for (int i = Character.charCount(cp); i < name.length(); i += Character.charCount(cp)) {
                    cp = name.codePointAt(i);
                    if (Character.isUpperCase(cp)) {
                        if (previousUpper) {
                            conventional = false;
                            break;
                        }
                        previousUpper = true;
                    } else {
                        previousUpper = false;
                    }
                }
            }

            if (!conventional)
                messager.printMessage(Diagnostic.Kind.WARNING, "名称 " + name + "应当符合驼峰式命名", e);
        }

        /**
         * 大写命名检查，要求第一个字母必须是大写的英文字母，其余部分呢可以是下划线或大写字母
         * @param e
         */
        private void checkAllCaps(Element e) {
            String name = e.getSimpleName().toString();

            boolean conventional = true;
            int firstCodePoint = name.codePointAt(0);

            if (!Character.isUpperCase(firstCodePoint)) {
                conventional = false;
            } else {
                boolean previousUnderscore = false;
                int cp = firstCodePoint;
                for (int i = Character.charCount(cp); i < name.length(); i += Character.charCount(cp)) {
                    cp = name.codePointAt(i);
                    if (cp == (int) '_') {
                        // 两个下划线不能同时存在
                        if (previousUnderscore) {
                            conventional = false;
                            break;
                        }
                        previousUnderscore = true;
                    } else {
                        previousUnderscore = false;
                        if (!Character.isUpperCase(cp) && !Character.isDigit(cp)) {
                            conventional = false;
                            break;
                        }
                    }
                }
            }

            if (!conventional)
                messager.printMessage(Diagnostic.Kind.WARNING, "常量 " + name + "应当全部以大写字母或者下划线命名，并且以字母开头", e);
        }
    }
}
