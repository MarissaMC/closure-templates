/*
 * Copyright 2015 Google Inc.
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

package com.google.template.soy.jbcsrc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.template.soy.data.SoyDataException;
import com.google.template.soy.data.SoyValue;
import com.google.template.soy.data.restricted.BooleanData;
import com.google.template.soy.data.restricted.FloatData;
import com.google.template.soy.data.restricted.IntegerData;
import com.google.template.soy.data.restricted.StringData;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.exprtree.FieldAccessNode;
import com.google.template.soy.exprtree.FunctionNode;
import com.google.template.soy.exprtree.ItemAccessNode;
import com.google.template.soy.exprtree.VarRefNode;
import com.google.template.soy.jbcsrc.ExpressionTester.ExpressionSubject;
import com.google.template.soy.jbcsrc.SoyExpression.BoolExpression;
import com.google.template.soy.shared.SharedTestUtils;
import com.google.template.soy.soytree.PrintNode;

import junit.framework.TestCase;

import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link ExpressionCompiler}
 */
public class ExpressionCompilerTest extends TestCase {
  private final Map<String, SoyExpression> variables = new HashMap<>();
  private ExpressionCompiler testExpressionCompiler = new ExpressionCompiler() {
    @Override protected SoyExpression visitVarRefNode(VarRefNode node) {
      SoyExpression expr = variables.get(node.getName());
      if (expr == null) {
        throw new UnsupportedOperationException();
      }
      return expr;
    }

    @Override protected SoyExpression visitItemAccessNode(ItemAccessNode node) {
      throw new UnsupportedOperationException();
    }

    @Override protected SoyExpression visitFunctionNode(FunctionNode node) {
      throw new UnsupportedOperationException();
    }

    @Override protected SoyExpression visitFieldAccessNode(FieldAccessNode node) {
      throw new UnsupportedOperationException();
    }
  };

  public void testConstants() {
    assertExpression("1").evaluatesTo(1L);
    assertExpression("1.0").evaluatesTo(1D);
    assertExpression("false").evaluatesTo(false);
    assertExpression("true").evaluatesTo(true);
    assertExpression("'asdf'").evaluatesTo("asdf");
  }

  public void testCollectionLiterals_list() {
    assertExpression("[]").evaluatesTo(ImmutableList.of());
    // Lists values are always boxed
    assertExpression("[1, 1.0, 'asdf', false]")
        .evaluatesTo(
            ImmutableList.of(
                IntegerData.forValue(1),
                FloatData.forValue(1.0),
                StringData.forValue("asdf"),
                BooleanData.FALSE));
  }

  public void testCollectionLiterals_map() {
    assertExpression("[:]").evaluatesTo(ImmutableMap.of());

    // Map values are always boxed
    assertExpression("['a': 1, 'b': 1.0, 'c': 'asdf', 'd': false]")
        .evaluatesTo(
            ImmutableMap.<String, SoyValue>of(
                "a", IntegerData.forValue(1),
                "b", FloatData.forValue(1.0),
                "c", StringData.forValue("asdf"),
                "d", BooleanData.FALSE));
  }

  public void testNegativeOpNode() {
    assertExpression("-1").evaluatesTo(-1L);
    assertExpression("-1.0").evaluatesTo(-1.0);
    assertExpression("-'asdf'").throwsExceptionOfType(SoyDataException.class);

    variables.put("foo", untypedBoxedSoyExpression(BytecodeUtils.constant(1L)));
    assertExpression("-$foo").evaluatesTo(IntegerData.forValue(-1));

    variables.put("foo", untypedBoxedSoyExpression(BytecodeUtils.constant(1D)));
    assertExpression("-$foo").evaluatesTo(FloatData.forValue(-1.0));
  }

  public void testModOpNode() {
    assertExpression("3 % 2").evaluatesTo(1L);
    assertExpression("5 % 3").evaluatesTo(2L);
    // TODO(user): the soy type checker should flag this, but it doesn't.
    try {
      compileExpression("5.0 % 3.0");
      fail();
    } catch (Exception expected) {}

    variables.put("foo", untypedBoxedSoyExpression(BytecodeUtils.constant(3L)));
    variables.put("bar", untypedBoxedSoyExpression(BytecodeUtils.constant(2L)));
    assertExpression("$foo % $bar").evaluatesTo(1L);

    variables.put("foo", untypedBoxedSoyExpression(BytecodeUtils.constant(3.0)));
    variables.put("bar", untypedBoxedSoyExpression(BytecodeUtils.constant(2.0)));
    assertExpression("$foo % $bar").throwsExceptionOfType(SoyDataException.class);
  }

  public void testDivideByOpNode() {
    assertExpression("3 / 2").evaluatesTo(1.5);  // note the coercion to floating point
    assertExpression("4.2 / 2").evaluatesTo(2.1);

    variables.put("foo", untypedBoxedSoyExpression(BytecodeUtils.constant(3.0)));
    variables.put("bar", untypedBoxedSoyExpression(BytecodeUtils.constant(2.0)));
    assertExpression("$foo / $bar").evaluatesTo(1.5);
  }

  public void testTimesOpNode() {
    assertExpression("4.2 * 2").evaluatesTo(8.4);
    assertExpression("4 * 2").evaluatesTo(8L);

    variables.put("foo", untypedBoxedSoyExpression(BytecodeUtils.constant(3.0)));
    variables.put("bar", untypedBoxedSoyExpression(BytecodeUtils.constant(2.0)));
    assertExpression("$foo * $bar").evaluatesTo(FloatData.forValue(6.0));
  }

  public void testMinusOpNode() {
    assertExpression("4.2 - 2").evaluatesTo(2.2);
    assertExpression("4 - 2").evaluatesTo(2L);

    variables.put("foo", untypedBoxedSoyExpression(BytecodeUtils.constant(3.0)));
    variables.put("bar", untypedBoxedSoyExpression(BytecodeUtils.constant(2.0)));
    assertExpression("$foo - $bar").evaluatesTo(FloatData.forValue(1.0));
  }

  public void testPlusOpNode() {
    assertExpression("4.2 + 2").evaluatesTo(6.2);
    assertExpression("4 + 2").evaluatesTo(6L);
    assertExpression("4 + '2'").evaluatesTo("42");
    assertExpression("'4' + 2").evaluatesTo("42");

    variables.put("foo", untypedBoxedSoyExpression(BytecodeUtils.constant("foo")));
    assertExpression("$foo + 2").evaluatesTo(StringData.forValue("foo2"));
    assertExpression("$foo + '2'").evaluatesTo("foo2");  // Note, not boxed

    variables.put("foo", untypedBoxedSoyExpression(BytecodeUtils.constant(1L)));
    assertExpression("$foo + 2").evaluatesTo(IntegerData.forValue(3));
    assertExpression("$foo + '2'").evaluatesTo("12");
  }

  public void testNotOpNode() {
    assertExpression("not false").evaluatesTo(true);
    assertExpression("not true").evaluatesTo(false);

    variables.put("foo", untypedBoxedSoyExpression(BytecodeUtils.constant("foo")));
    assertExpression("not $foo").evaluatesTo(false);

    variables.put("foo", untypedBoxedSoyExpression(BytecodeUtils.constant("")));
    assertExpression("not $foo").evaluatesTo(true);  // empty string is falsy
  }

  public void testComparisonOperators() {
    variables.put("oneInt", untypedBoxedSoyExpression(BytecodeUtils.constant(1L)));
    variables.put("oneFloat", untypedBoxedSoyExpression(BytecodeUtils.constant(1.0)));
    variables.put("twoInt", untypedBoxedSoyExpression(BytecodeUtils.constant(2L)));
    variables.put("twoFloat", untypedBoxedSoyExpression(BytecodeUtils.constant(2.0)));

    for (String one : ImmutableList.of("1", "1.0", "$oneInt", "$oneFloat")) {
      for (String two : ImmutableList.of("2", "2.0", "$twoInt", "$twoFloat")) {
        assertExpression(one + " < " + two).evaluatesTo(true);
        assertExpression(one + " < " + one).evaluatesTo(false);
        assertExpression(two + " < " + one).evaluatesTo(false);

        assertExpression(one + " <= " + two).evaluatesTo(true);
        assertExpression(one + " <= " + one).evaluatesTo(true);
        assertExpression(two + " <= " + one).evaluatesTo(false);

        assertExpression(one + " > " + two).evaluatesTo(false);
        assertExpression(one + " > " + one).evaluatesTo(false);
        assertExpression(two + " > " + one).evaluatesTo(true);

        assertExpression(one + " >= " + two).evaluatesTo(false);
        assertExpression(one + " >= " + one).evaluatesTo(true);
        assertExpression(two + " >= " + one).evaluatesTo(true);

        assertExpression(one + " == " + two).evaluatesTo(false);
        assertExpression(one + " == " + one).evaluatesTo(true);
        assertExpression(two + " == " + one).evaluatesTo(false);

        assertExpression(one + " != " + two).evaluatesTo(true);
        assertExpression(one + " != " + one).evaluatesTo(false);
        assertExpression(two + " != " + one).evaluatesTo(true);
      }
    }
  }

  public void testConditionalOperatorse() {
    variables.put("true", untypedBoxedSoyExpression(BoolExpression.TRUE));
    variables.put("false", untypedBoxedSoyExpression(BoolExpression.FALSE));

    for (String trueExpr : ImmutableList.of("true", "$true")) {
      for (String falseExpr : ImmutableList.of("false", "$false")) {
        assertExpression(falseExpr + " or " + falseExpr).evaluatesTo(false);
        assertExpression(falseExpr + " or " + trueExpr).evaluatesTo(true);
        assertExpression(trueExpr + " or " + falseExpr).evaluatesTo(true);
        assertExpression(trueExpr + " or " + trueExpr).evaluatesTo(true);

        assertExpression(falseExpr + " and " + falseExpr).evaluatesTo(false);
        assertExpression(falseExpr + " and " + trueExpr).evaluatesTo(false);
        assertExpression(trueExpr + " and " + falseExpr).evaluatesTo(false);
        assertExpression(trueExpr + " and " + trueExpr).evaluatesTo(true);
      }
    }
  }

  // The arithmetic types are handled by testComparisonOperators, the == and != operators have
  // extra semantics for strings as well as boxed fallback
  public void testEqualOpNode() {
    assertExprNotEquals("'asdf'", "12.0");
    assertExprEquals("'12'", "12.0");
    assertExprEquals("'12.0'", "12.0");
    assertExprEquals("'12.0'", "'12.0'");
    assertExprEquals("'asdf'", "'asdf'");

    variables.put("str", untypedBoxedSoyExpression(BytecodeUtils.constant("foo")));
    assertExprEquals("$str", "'foo'");
    assertExprNotEquals("$str", "'bar'");

    variables.put("intStr", untypedBoxedSoyExpression(BytecodeUtils.constant("12")));
    assertExprEquals("$intStr", "'12'");
    assertExprEquals("$intStr", "12");
    assertExprNotEquals("$intStr", "'bar'");

    variables.put("floatStr", untypedBoxedSoyExpression(BytecodeUtils.constant(12.0)));
    assertExprEquals("$floatStr", "'12'");
    assertExprEquals("$floatStr", "12");
    assertExprNotEquals("$floatStr", "'bar'");
  }

  public void testConditionalOpNode() {
    assertExpression("false ? 1 : 2").evaluatesTo(2L);
    assertExpression("true ? 1 : 2").evaluatesTo(1L);

    assertExpression("false ? 1.0 : 2").evaluatesTo(2.0);
    assertExpression("true ? 1 : 2.0").evaluatesTo(1.0);

    assertExpression("false ? 'a' : 'b'").evaluatesTo("b");
    assertExpression("true ? 'a' : 'b'").evaluatesTo("a");

    // note the boxing
    assertExpression("false ? 'a' : 2").evaluatesTo(IntegerData.forValue(2));
    assertExpression("true ? 1 : 'b'").evaluatesTo(IntegerData.forValue(1));
    assertExpression("false ? 1 : 'b'").evaluatesTo(StringData.forValue("b"));
    assertExpression("true ? 'a' : 2").evaluatesTo(StringData.forValue("a"));
  }

  private void assertExprEquals(String left, String right) {
    assertExpression(left + " == " + right).evaluatesTo(true);
    assertExpression(left + " != " + right).evaluatesTo(false);
  }

  private void assertExprNotEquals(String left, String right) {
    assertExpression(left + " == " + right).evaluatesTo(false);
    assertExpression(left + " != " + right).evaluatesTo(true);
  }

  private ExpressionSubject assertExpression(String soyExpr) {
    SoyExpression compile = compileExpression(soyExpr);
    return ExpressionTester.assertThatExpression(compile);
  }

  private SoyExpression compileExpression(String soyExpr) {
    PrintNode code =
        (PrintNode) SharedTestUtils.parseSoyCode("{" + soyExpr + "}")
        .getParseTree()
        .getChild(0)
        .getChild(0)
        .getChild(0);
    ExprRootNode<?> expr = code.getExprUnion().getExpr();
    SoyExpression compile = testExpressionCompiler.compile(expr);
    return compile;
  }

  /**
   * This helper can take a SoyExpression and essentially strip type information from it, this is
   * useful for testing fallback implementations in the compiler.
   */
  private SoyExpression untypedBoxedSoyExpression(final SoyExpression expr) {
    return new SoyExpression(SoyValue.class) {
      @Override SoyExpression box() {
        return this;
      }

      @Override void gen(GeneratorAdapter adapter) {
        expr.box().gen(adapter);
      }
    };
  }
}
