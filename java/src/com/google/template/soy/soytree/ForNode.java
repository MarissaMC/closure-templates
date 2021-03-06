/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.soytree;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.exprparse.ExpressionParser;
import com.google.template.soy.exprtree.ExprNode;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.soyparse.ErrorReporter;
import com.google.template.soy.soyparse.SoyError;
import com.google.template.soy.soytree.SoyNode.ConditionalBlockNode;
import com.google.template.soy.soytree.SoyNode.ExprHolderNode;
import com.google.template.soy.soytree.SoyNode.LocalVarBlockNode;
import com.google.template.soy.soytree.SoyNode.LoopNode;
import com.google.template.soy.soytree.SoyNode.StandaloneNode;
import com.google.template.soy.soytree.SoyNode.StatementNode;
import com.google.template.soy.soytree.defn.LocalVar;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Node representing a 'for' statement.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
public final class ForNode extends AbstractBlockCommandNode
    implements StandaloneNode, StatementNode, ConditionalBlockNode, LoopNode, ExprHolderNode,
    LocalVarBlockNode {

  private static final SoyError INVALID_COMMAND_TEXT
      = SoyError.of("Invalid ''for'' command text");
  private static final SoyError INVALID_RANGE_SPECIFICATION
      = SoyError.of("Invalid range specification");

  /** Regex pattern for the command text. */
  // 2 capturing groups: local var name, arguments to range()
  private static final Pattern COMMAND_TEXT_PATTERN =
      Pattern.compile("( [$] \\w+ ) \\s+ in \\s+ range[(] \\s* (.*) \\s* [)]",
                      Pattern.COMMENTS | Pattern.DOTALL);


  /** The Local variable for this loop. */
  private final LocalVar var;

  /** The texts of the individual range args (sort of canonicalized). */
  private final ImmutableList<String> rangeArgTexts;

  /** The parsed range args. */
  private final ImmutableList<ExprRootNode<?>> rangeArgs;


  /**
   * @param id The id for this node.
   * @param commandText The command text.
   * @param sourceLocation The source location for the {@code for }node.
   * @param errorReporter For reporting errors.
   */
  public ForNode(
      int id,
      String commandText,
      SourceLocation sourceLocation,
      ErrorReporter errorReporter) {
    super(id, "for", commandText);
    setSourceLocation(sourceLocation);

    Matcher matcher = COMMAND_TEXT_PATTERN.matcher(commandText);
    if (!matcher.matches()) {
      errorReporter.report(sourceLocation, INVALID_COMMAND_TEXT);
    }

    String varName = parseVarName(
        matcher.group(1), sourceLocation, errorReporter);
    List<ExprRootNode<?>> tempRangeArgs = parseRangeArgs(
        matcher.group(2), sourceLocation, errorReporter);

    if (tempRangeArgs.size() > 3) {
      errorReporter.report(sourceLocation, INVALID_RANGE_SPECIFICATION);
    }
    rangeArgs = ImmutableList.copyOf(tempRangeArgs);

    List<String> tempRangeArgTexts = Lists.newArrayList();
    for (ExprNode rangeArg : rangeArgs) {
      tempRangeArgTexts.add(rangeArg.toSourceString());
    }
    rangeArgTexts = ImmutableList.copyOf(tempRangeArgTexts);
    var = new LocalVar(varName, this, null);
  }

  private static String parseVarName(
      String input, SourceLocation sourceLocation, ErrorReporter errorReporter) {
    return new ExpressionParser(input, sourceLocation, errorReporter)
        .parseVariable()
        .getChild(0)
        .getName();
  }

  private static List<ExprRootNode<?>> parseRangeArgs(
      String input, SourceLocation sourceLocation, ErrorReporter errorReporter) {
    return new ExpressionParser(input, sourceLocation, errorReporter)
        .parseExpressionList();
  }


  /**
   * Copy constructor.
   * @param orig The node to copy.
   */
  private ForNode(ForNode orig) {
    super(orig);
    this.var = orig.var.clone();
    this.rangeArgTexts = orig.rangeArgTexts;  // safe to reuse (immutable)
    List<ExprRootNode<?>> tempRangeArgs =
        Lists.newArrayListWithCapacity(orig.rangeArgs.size());
    for (ExprRootNode<?> origRangeArg : orig.rangeArgs) {
      tempRangeArgs.add(origRangeArg.clone());
    }
    this.rangeArgs = ImmutableList.copyOf(tempRangeArgs);
  }


  @Override public Kind getKind() {
    return Kind.FOR_NODE;
  }


  public final LocalVar getVar() {
    return var;
  }


  @Override public final String getVarName() {
    return var.name();
  }


  /** Returns the texts of the individual range args (sort of canonicalized). */
  public List<String> getRangeArgTexts() {
    return rangeArgTexts;
  }


  /** Returns the parsed range args. */
  public List<ExprRootNode<?>> getRangeArgs() {
    return rangeArgs;
  }


  @Override public List<ExprUnion> getAllExprUnions() {
    return ExprUnion.createList(rangeArgs);
  }


  @Override public BlockNode getParent() {
    return (BlockNode) super.getParent();
  }


  @Override public ForNode clone() {
    return new ForNode(this);
  }

}
