// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.javascript.refactoring.FormatFixer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.util.ObjectUtils;
import org.angular2.lang.html.psi.Angular2HtmlElementVisitor;
import org.angular2.lang.html.psi.Angular2HtmlPropertyBinding;
import org.angular2.lang.html.psi.PropertyBindingType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class Angular2AnimationTriggerAssignmentInspection extends LocalInspectionTool {

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new Angular2HtmlElementVisitor() {
      @Override
      public void visitPropertyBinding(Angular2HtmlPropertyBinding propertyBinding) {
        if (propertyBinding.getBindingType() == PropertyBindingType.ANIMATION
            && propertyBinding.getName().startsWith("@")
            && propertyBinding.getValueElement() != null
            && !StringUtil.notNullize(propertyBinding.getValue()).isEmpty()) {
          holder.registerProblem(propertyBinding.getValueElement(),
                                 "Assigning animation triggers via @prop=\"exp\" attributes with an expression is invalid.",
                                 new ConvertToPropertyBindingQuickFix(propertyBinding.getPropertyName()),
                                 new RemoveAttributeValueQuickFix());
        }
      }
    };
  }

  private static class ConvertToPropertyBindingQuickFix implements LocalQuickFix {

    private final String myAnimationTrigger;

    private ConvertToPropertyBindingQuickFix(@NotNull String animationTrigger) {
      myAnimationTrigger = animationTrigger;
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
      return "Bind to property [@" + myAnimationTrigger + "]";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
      return "Angular";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      final XmlAttribute attribute = ObjectUtils.tryCast(descriptor.getPsiElement().getParent(), XmlAttribute.class);
      if (attribute != null) {
        attribute.setName("[@" + myAnimationTrigger + "]");
      }
    }
  }

  private static class RemoveAttributeValueQuickFix implements LocalQuickFix {

    @Nls
    @NotNull
    @Override
    public String getName() {
      return "Remove attribute value";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
      return "Angular";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      final XmlAttribute attribute = ObjectUtils.tryCast(descriptor.getPsiElement().getParent(), XmlAttribute.class);
      if (attribute != null && attribute.getValueElement() != null) {
        attribute.deleteChildRange(attribute.getNameElement().getNextSibling(),
                                   attribute.getValueElement());
        FormatFixer.create(attribute, FormatFixer.Mode.Reformat).fixFormat();
      }
    }
  }
}
