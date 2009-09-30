/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.refactorings.extractstring;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.text.edits.TextEditGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Visitor used by {@link ExtractStringRefactoring} to extract a string from an existing
 * Java source and replace it by an Android XML string reference.
 *
 * @see ExtractStringRefactoring#computeJavaChanges
 */
class ReplaceStringsVisitor extends ASTVisitor {

    private static final String CLASS_ANDROID_CONTEXT    = "android.content.Context"; //$NON-NLS-1$
    private static final String CLASS_JAVA_CHAR_SEQUENCE = "java.lang.CharSequence";  //$NON-NLS-1$
    private static final String CLASS_JAVA_STRING        = "java.lang.String";        //$NON-NLS-1$


    private final AST mAst;
    private final ASTRewrite mRewriter;
    private final String mOldString;
    private final String mRQualifier;
    private final String mXmlId;
    private final ArrayList<TextEditGroup> mEditGroups;

    public ReplaceStringsVisitor(AST ast,
            ASTRewrite astRewrite,
            ArrayList<TextEditGroup> editGroups,
            String oldString,
            String rQualifier,
            String xmlId) {
        mAst = ast;
        mRewriter = astRewrite;
        mEditGroups = editGroups;
        mOldString = oldString;
        mRQualifier = rQualifier;
        mXmlId = xmlId;
    }

    @SuppressWarnings("unchecked")    //$NON-NLS-1$
    @Override
    public boolean visit(StringLiteral node) {
        if (node.getLiteralValue().equals(mOldString)) {

            // We want to analyze the calling context to understand whether we can
            // just replace the string literal by the named int constant (R.id.foo)
            // or if we should generate a Context.getString() call.
            boolean useGetResource = false;
            useGetResource = examineVariableDeclaration(node) ||
                                examineMethodInvocation(node);

            Name qualifierName = mAst.newName(mRQualifier + ".string");     //$NON-NLS-1$
            SimpleName idName = mAst.newSimpleName(mXmlId);
            ASTNode newNode = mAst.newQualifiedName(qualifierName, idName);
            String title = "Replace string by ID";

            if (useGetResource) {

                Expression context = methodHasContextArgument(node);
                if (context == null && !isClassDerivedFromContext(node)) {
                    // if we don't have a class that derives from Context and
                    // we don't have a Context method argument, then try a bit harder:
                    // can we find a method or a field that will give us a context?
                    context = findContextFieldOrMethod(node);

                    if (context == null) {
                        // If not, let's  write Context.getString(), which is technically
                        // invalid but makes it a good clue on how to fix it.
                        context = mAst.newSimpleName("Context");            //$NON-NLS-1$
                    }
                }

                MethodInvocation mi2 = mAst.newMethodInvocation();
                mi2.setName(mAst.newSimpleName("getString"));               //$NON-NLS-1$
                mi2.setExpression(context);
                mi2.arguments().add(newNode);

                newNode = mi2;
                title = "Replace string by Context.getString(R.string...)";
            }

            TextEditGroup editGroup = new TextEditGroup(title);
            mEditGroups.add(editGroup);
            mRewriter.replace(node, newNode, editGroup);
        }
        return super.visit(node);
    }

    /**
     * Examines if the StringLiteral is part of of an assignment to a string,
     * e.g. String foo = id.
     *
     * The parent fragment is of syntax "var = expr" or "var[] = expr".
     * We want the type of the variable, which is either held by a
     * VariableDeclarationStatement ("type [fragment]") or by a
     * VariableDeclarationExpression. In either case, the type can be an array
     * but for us all that matters is to know whether the type is an int or
     * a string.
     */
    private boolean examineVariableDeclaration(StringLiteral node) {
        VariableDeclarationFragment fragment = findParentClass(node,
                VariableDeclarationFragment.class);

        if (fragment != null) {
            ASTNode parent = fragment.getParent();

            Type type = null;
            if (parent instanceof VariableDeclarationStatement) {
                type = ((VariableDeclarationStatement) parent).getType();
            } else if (parent instanceof VariableDeclarationExpression) {
                type = ((VariableDeclarationExpression) parent).getType();
            }

            if (type instanceof SimpleType) {
                return isJavaString(type.resolveBinding());
            }
        }

        return false;
    }

    /**
     * If the expression is part of a method invocation (aka a function call) or a
     * class instance creation (aka a "new SomeClass" constructor call), we try to
     * find the type of the argument being used. If it is a String (most likely), we
     * want to return true (to generate a getString() call). However if there might
     * be a similar method that takes an int, in which case we don't want to do that.
     *
     * This covers the case of Activity.setTitle(int resId) vs setTitle(String str).
     */
    @SuppressWarnings("unchecked")  //$NON-NLS-1$
    private boolean examineMethodInvocation(StringLiteral node) {

        ASTNode parent = null;
        List arguments = null;
        IMethodBinding methodBinding = null;

        MethodInvocation invoke = findParentClass(node, MethodInvocation.class);
        if (invoke != null) {
            parent = invoke;
            arguments = invoke.arguments();
            methodBinding = invoke.resolveMethodBinding();
        } else {
            ClassInstanceCreation newclass = findParentClass(node, ClassInstanceCreation.class);
            if (newclass != null) {
                parent = newclass;
                arguments = newclass.arguments();
                methodBinding = newclass.resolveConstructorBinding();
            }
        }

        if (parent != null && arguments != null && methodBinding != null) {
            // We want to know which argument this is.
            // Walk up the hierarchy again to find the immediate child of the parent,
            // which should turn out to be one of the invocation arguments.
            ASTNode child = null;
            for (ASTNode n = node; n != parent; ) {
                ASTNode p = n.getParent();
                if (p == parent) {
                    child = n;
                    break;
                }
                n = p;
            }
            if (child == null) {
                // This can't happen: a parent of 'node' must be the child of 'parent'.
                return false;
            }

            // Find the index
            int index = 0;
            for (Object arg : arguments) {
                if (arg == child) {
                    break;
                }
                index++;
            }

            if (index == arguments.size()) {
                // This can't happen: one of the arguments of 'invoke' must be 'child'.
                return false;
            }

            // Eventually we want to determine if the parameter is a string type,
            // in which case a Context.getString() call must be generated.
            boolean useStringType = false;

            // Find the type of that argument
            ITypeBinding[] types = methodBinding.getParameterTypes();
            if (index < types.length) {
                ITypeBinding type = types[index];
                useStringType = isJavaString(type);
            }

            // Now that we know that this method takes a String parameter, can we find
            // a variant that would accept an int for the same parameter position?
            if (useStringType) {
                String name = methodBinding.getName();
                ITypeBinding clazz = methodBinding.getDeclaringClass();
                nextMethod: for (IMethodBinding mb2 : clazz.getDeclaredMethods()) {
                    if (methodBinding == mb2 || !mb2.getName().equals(name)) {
                        continue;
                    }
                    // We found a method with the same name. We want the same parameters
                    // except that the one at 'index' must be an int type.
                    ITypeBinding[] types2 = mb2.getParameterTypes();
                    int len2 = types2.length;
                    if (types.length == len2) {
                        for (int i = 0; i < len2; i++) {
                            if (i == index) {
                                ITypeBinding type2 = types2[i];
                                if (!("int".equals(type2.getQualifiedName()))) {   //$NON-NLS-1$
                                    // The argument at 'index' is not an int.
                                    continue nextMethod;
                                }
                            } else if (!types[i].equals(types2[i])) {
                                // One of the other arguments do not match our original method
                                continue nextMethod;
                            }
                        }
                        // If we got here, we found a perfect match: a method with the same
                        // arguments except the one at 'index' is an int. In this case we
                        // don't need to convert our R.id into a string.
                        useStringType = false;
                        break;
                    }
                }
            }

            return useStringType;
        }
        return false;
    }

    /**
     * Examines if the StringLiteral is part of a method declaration (a.k.a. a function
     * definition) which takes a Context argument.
     * If such, it returns the name of the variable as a {@link SimpleName}.
     * Otherwise it returns null.
     */
    private SimpleName methodHasContextArgument(StringLiteral node) {
        MethodDeclaration decl = findParentClass(node, MethodDeclaration.class);
        if (decl != null) {
            for (Object obj : decl.parameters()) {
                if (obj instanceof SingleVariableDeclaration) {
                    SingleVariableDeclaration var = (SingleVariableDeclaration) obj;
                    if (isAndroidContext(var.getType())) {
                        return mAst.newSimpleName(var.getName().getIdentifier());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Walks up the node hierarchy to find the class (aka type) where this statement
     * is used and returns true if this class derives from android.content.Context.
     */
    private boolean isClassDerivedFromContext(StringLiteral node) {
        TypeDeclaration clazz = findParentClass(node, TypeDeclaration.class);
        if (clazz != null) {
            // This is the class that the user is currently writing, so it can't be
            // a Context by itself, it has to be derived from it.
            return isAndroidContext(clazz.getSuperclassType());
        }
        return false;
    }

    private Expression findContextFieldOrMethod(StringLiteral node) {
        TypeDeclaration clazz = findParentClass(node, TypeDeclaration.class);
        ITypeBinding clazzType = clazz == null ? null : clazz.resolveBinding();
        return findContextFieldOrMethod(clazzType);
    }

    private Expression findContextFieldOrMethod(ITypeBinding clazzType) {
        TreeMap<Integer, Expression> results = new TreeMap<Integer, Expression>();
        findContextCandidates(results, clazzType, 0 /*superType*/);
        if (results.size() > 0) {
            Integer bestRating = results.keySet().iterator().next();
            return results.get(bestRating);
        }
        return null;
    }

    /**
     * Find all method or fields that are candidates for providing a Context.
     * There can be various choices amongst this class or its super classes.
     * Sort them by rating in the results map.
     *
     * The best ever choice is to find a method with no argument that returns a Context.
     * The second suitable choice is to find a Context field.
     * The least desirable choice is to find a method with arguments. It's not really
     * desirable since we can't generate these arguments automatically.
     *
     * Methods and fields from supertypes are ignored if they are private.
     *
     * The rating is reversed: the lowest rating integer is used for the best candidate.
     * Because the superType argument is actually a recursion index, this makes the most
     * immediate classes more desirable.
     *
     * @param results The map that accumulates the rating=>expression results. The lower
     *                rating number is the best candidate.
     * @param clazzType The class examined.
     * @param superType The recursion index.
     *                  0 for the immediate class, 1 for its super class, etc.
     */
    private void findContextCandidates(TreeMap<Integer, Expression> results,
            ITypeBinding clazzType,
            int superType) {
        for (IMethodBinding mb : clazzType.getDeclaredMethods()) {
            // If we're looking at supertypes, we can't use private methods.
            if (superType != 0 && Modifier.isPrivate(mb.getModifiers())) {
                continue;
            }

            if (isAndroidContext(mb.getReturnType())) {
                // We found a method that returns something derived from Context.

                int argsLen = mb.getParameterTypes().length;
                if (argsLen == 0) {
                    // We'll favor any method that takes no argument,
                    // That would be the best candidate ever, so we can stop here.
                    MethodInvocation mi = mAst.newMethodInvocation();
                    mi.setName(mAst.newSimpleName(mb.getName()));
                    results.put(Integer.MIN_VALUE, mi);
                    return;
                } else {
                    // A method with arguments isn't as interesting since we wouldn't
                    // know how to populate such arguments. We'll use it if there are
                    // no other alternatives. We'll favor the one with the less arguments.
                    Integer rating = Integer.valueOf(10000 + 1000 * superType + argsLen);
                    if (!results.containsKey(rating)) {
                        MethodInvocation mi = mAst.newMethodInvocation();
                        mi.setName(mAst.newSimpleName(mb.getName()));
                        results.put(rating, mi);
                    }
                }
            }
        }

        // A direct Context field would be more interesting than a method with
        // arguments. Try to find one.
        for (IVariableBinding var : clazzType.getDeclaredFields()) {
            // If we're looking at supertypes, we can't use private field.
            if (superType != 0 && Modifier.isPrivate(var.getModifiers())) {
                continue;
            }

            if (isAndroidContext(var.getType())) {
                // We found such a field. Let's use it.
                Integer rating = Integer.valueOf(superType);
                results.put(rating, mAst.newSimpleName(var.getName()));
                break;
            }
        }

        // Examine the super class to see if we can locate a better match
        clazzType = clazzType.getSuperclass();
        if (clazzType != null) {
            findContextCandidates(results, clazzType, superType + 1);
        }
    }

    /**
     * Walks up the node hierarchy and returns the first ASTNode of the requested class.
     * Only look at parents.
     *
     * Implementation note: this is a generic method so that it returns the node already
     * casted to the requested type.
     */
    @SuppressWarnings("unchecked")
    private <T extends ASTNode> T findParentClass(ASTNode node, Class<T> clazz) {
        for (node = node.getParent(); node != null; node = node.getParent()) {
            if (node.getClass().equals(clazz)) {
                return (T) node;
            }
        }
        return null;
    }

    /**
     * Returns true if the given type is or derives from android.content.Context.
     */
    private boolean isAndroidContext(Type type) {
        if (type != null) {
            return isAndroidContext(type.resolveBinding());
        }
        return false;
    }

    /**
     * Returns true if the given type is or derives from android.content.Context.
     */
    private boolean isAndroidContext(ITypeBinding type) {
        for (; type != null; type = type.getSuperclass()) {
            if (CLASS_ANDROID_CONTEXT.equals(type.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this type binding represents a String or CharSequence type.
     */
    private boolean isJavaString(ITypeBinding type) {
        for (; type != null; type = type.getSuperclass()) {
            if (CLASS_JAVA_STRING.equals(type.getQualifiedName()) ||
                CLASS_JAVA_CHAR_SEQUENCE.equals(type.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }
}
