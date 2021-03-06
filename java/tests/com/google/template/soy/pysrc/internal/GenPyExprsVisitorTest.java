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

package com.google.template.soy.pysrc.internal;

import static com.google.template.soy.pysrc.internal.SoyExprForPySubject.assertThatSoyExpr;

import com.google.template.soy.exprtree.Operator;
import com.google.template.soy.pysrc.restricted.PyExpr;
import com.google.template.soy.pysrc.restricted.PyExprUtils;

import junit.framework.TestCase;

/**
 * Unit tests for GenPyExprsVisitor.
 *
 */
public final class GenPyExprsVisitorTest extends TestCase {

  public void testRawText() {
    assertThatSoyExpr("I'm feeling lucky!").compilesTo(
        new PyExpr("'I\\'m feeling lucky!'", Integer.MAX_VALUE));
  }

  public void testIf() {
    String soyNodeCode =
        "{if $boo}\n"
      + "  Blah\n"
      + "{elseif not $goo}\n"
      + "  Bleh\n"
      + "{else}\n"
      + "  Bluh\n"
      + "{/if}\n";
    String expectedPyExprText =
        "'Blah' if opt_data.get('boo') else 'Bleh' if not opt_data.get('goo') else 'Bluh'";

    assertThatSoyExpr(soyNodeCode).compilesTo(
        new PyExpr(expectedPyExprText,
            PyExprUtils.pyPrecedenceForOperator(Operator.CONDITIONAL)));
  }

  public void testIf_nested() {
    String soyNodeCode =
        "{if $boo}\n"
      + "  {if $goo}\n"
      + "    Blah\n"
      + "  {/if}\n"
      + "{else}\n"
      + "  Bleh\n"
      + "{/if}\n";
    String expectedPyExprText =
        "('Blah' if opt_data.get('goo') else '') if opt_data.get('boo') else 'Bleh'";

    assertThatSoyExpr(soyNodeCode).compilesTo(
        new PyExpr(expectedPyExprText,
            PyExprUtils.pyPrecedenceForOperator(Operator.CONDITIONAL)));
  }

  public void testSimpleMsgFallbackGroupNodeWithOneNode() {
    String soyCode =
          "{msg meaning=\"verb\" desc=\"Used as a verb.\"}\n"
        + "  Archive\n"
        + "{/msg}\n";

    String expectedPyCode =
        "translator_impl.render_literal("
        + "translator_impl.prepare_literal("
          + "###, "
          + "'Archive', "
          + "meaning='verb'))";

    assertThatSoyExpr(soyCode).compilesTo(new PyExpr(expectedPyCode, Integer.MAX_VALUE));
  }

  public void testMsgFallbackGroupNodeWithTwoNodes() {
    String soyCode =
          "{msg meaning=\"verb\" desc=\"Used as a verb.\"}\n"
        + "  archive\n"
        + "{fallbackmsg desc=\"\"}\n"
        + "  ARCHIVE\n"
        + "{/msg}\n";

    String expectedPyCode =
        "translator_impl.render_literal("
        + "translator_impl.prepare_literal("
          + "###, "
          + "'archive', "
          + "meaning='verb')) "
      + "if is_msg_available(###) else translator_impl.render_literal("
        + "translator_impl.prepare_literal(###, 'ARCHIVE'))";

    assertThatSoyExpr(soyCode).compilesTo(new PyExpr(expectedPyCode,
        PyExprUtils.pyPrecedenceForOperator(Operator.CONDITIONAL)));
  }

  public void testMsgOnlyLiteral() {
    String soyCode =
        "{msg meaning=\"verb\" desc=\"The word 'Archive' used as a verb.\"}"
          + "Archive"
      + "{/msg}\n";

    String expectedPyCode =
        "translator_impl.render_literal("
        + "translator_impl.prepare_literal("
          + "###, "
          + "'Archive', "
          + "meaning='verb'))";

    assertThatSoyExpr(soyCode).compilesTo(new PyExpr(expectedPyCode, Integer.MAX_VALUE));
  }

  public void testMsgSimpleSoyExpression() {
    String soyCode =
        "{msg desc=\"var placeholder\"}"
        + "Hello {$username}"
      + "{/msg}\n";

    String expectedPyCode =
        "translator_impl.render("
        + "translator_impl.prepare("
        + "###, "
        + "'Hello {USERNAME}', "
        + "('USERNAME',)), "
        + "{'USERNAME': str(opt_data.get('username'))})";

    assertThatSoyExpr(soyCode).compilesTo(new PyExpr(expectedPyCode, Integer.MAX_VALUE));
  }

  public void testMsgMultipleSoyExpressions() {
    String soyCode =
        "{msg desc=\"var placeholder\"}"
        + "{$greet} {$username}"
      + "{/msg}\n";

    String expectedPyCode =
        "translator_impl.render("
        + "translator_impl.prepare("
        + "###, "
        + "'{GREET} {USERNAME}', "
        + "('GREET', 'USERNAME')), "
        + "{"
          + "'GREET': str(opt_data.get('greet')), "
          + "'USERNAME': str(opt_data.get('username'))"
        + "})";

    assertThatSoyExpr(soyCode).compilesTo(new PyExpr(expectedPyCode, Integer.MAX_VALUE));
  }

  public void testMsgNamespacedSoyExpression() {
    String soyCode =
        "{msg desc=\"placeholder with namespace\"}"
        + "Hello {$foo.bar}"
      + "{/msg}\n";

    String expectedPyCode =
        "translator_impl.render("
        + "translator_impl.prepare("
        + "###, "
        + "'Hello {BAR}', "
        + "('BAR',)), "
        + "{'BAR': str(opt_data.get('foo').get('bar'))})";

    assertThatSoyExpr(soyCode).compilesTo(new PyExpr(expectedPyCode, Integer.MAX_VALUE));
  }
  public void testMsgWithArithmeticExpression() {
    String soyCode =
        "{msg desc=\"var placeholder\"}"
        + "Hello {$username + 1}"
      + "{/msg}\n";

    String expectedPyCode =
        "translator_impl.render("
        + "translator_impl.prepare("
        + "###, "
        + "'Hello {XXX}', "
        + "('XXX',)), "
        + "{'XXX': str(runtime.type_safe_add(opt_data.get('username'), 1))})";

    assertThatSoyExpr(soyCode).compilesTo(new PyExpr(expectedPyCode, Integer.MAX_VALUE));
  }
  public void testMsgWithHtmlNode() {
    // msg with HTML tags and raw texts
    String soyCode =
        "{msg desc=\"with link\"}"
        + "Please click <a href='{$url}'>here</a>."
      + "{/msg}";

    String expectedPyCode =
        "translator_impl.render("
        + "translator_impl.prepare("
          + "###, "
          + "'Please click {START_LINK}here{END_LINK}.', "
          + "('START_LINK', 'END_LINK')), "
          + "{"
            + "'START_LINK': ''.join(['<a href=\\'',str(opt_data.get('url')),'\\'>']), "
            + "'END_LINK': '</a>'"
          + "})";

    assertThatSoyExpr(soyCode).compilesTo(new PyExpr(expectedPyCode, Integer.MAX_VALUE));
  }

  public void testMsgWithPlural() {
    String soyCode =
        "{msg desc=\"simple plural\"}"
        + "{plural $numDrafts}"
          + "{case 0}No drafts"
          + "{case 1}1 draft"
          + "{default}{$numDrafts} drafts"
        + "{/plural}"
      + "{/msg}";

    String expectedPyCode =
        "translator_impl.render_plural("
        + "translator_impl.prepare_plural("
          + "###, "
          + "{"
            + "'=0': 'No drafts', "
            + "'=1': '1 draft', "
            + "'other': '{NUM_DRAFTS_2} drafts'"
          + "}, "
          + "('NUM_DRAFTS_1', 'NUM_DRAFTS_2')), "
        + "opt_data.get('numDrafts'), "
        + "{"
          + "'NUM_DRAFTS_1': opt_data.get('numDrafts'), "
          + "'NUM_DRAFTS_2': str(opt_data.get('numDrafts'))"
        + "})";

    assertThatSoyExpr(soyCode).compilesTo(new PyExpr(expectedPyCode, Integer.MAX_VALUE));
  }

  public void testMsgWithPluralAndOffset() {
    String soyCode =
        "{msg desc=\"offset plural\"}"
        + "{plural $numDrafts offset=\"2\"}"
          + "{case 0}No drafts"
          + "{case 1}1 draft"
          + "{default}{remainder($numDrafts)} drafts"
        + "{/plural}"
      + "{/msg}";

    String expectedPyCode =
        "translator_impl.render_plural("
        + "translator_impl.prepare_plural("
          + "###, "
          + "{"
            + "'=0': 'No drafts', "
            + "'=1': '1 draft', "
            + "'other': '{XXX} drafts'"
          + "}, "
          + "('NUM_DRAFTS', 'XXX')), "
        + "opt_data.get('numDrafts'), "
        + "{"
          + "'NUM_DRAFTS': opt_data.get('numDrafts'), "
          + "'XXX': str(opt_data.get('numDrafts') - 2)"
        + "})";

    assertThatSoyExpr(soyCode).compilesTo(new PyExpr(expectedPyCode, Integer.MAX_VALUE));
  }

  public void testMsgWithSelect() {
    String soyCode =
        "{msg desc=\"...\"}\n"
        + "  {select $userGender}\n"
        + "    {case 'female'}\n"
        + "      {select $targetGender}\n"
        + "        {case 'female'}Reply to her.{case 'male'}Reply to him.{default}Reply to them.\n"
        + "      {/select}\n"
        + "    {case 'male'}\n"
        + "      {select $targetGender}\n"
        + "        {case 'female'}Reply to her.{case 'male'}Reply to him.{default}Reply to them.\n"
        + "      {/select}\n"
        + "    {default}\n"
        + "      {select $targetGender}\n"
        + "        {case 'female'}Reply to her.{case 'male'}Reply to him.{default}Reply to them.\n"
        + "      {/select}\n"
        + "   {/select}\n"
        + "{/msg}\n";

    String expectedPyCode =
        "translator_impl.render_icu("
        + "translator_impl.prepare_icu("
          + "###, "
          + "'{USER_GENDER,select,"
            + "female{"
              + "{TARGET_GENDER,select,"
              + "female{Reply to her.}"
              + "male{Reply to him.}"
              + "other{Reply to them.}}"
            + "}"
            + "male{"
              + "{TARGET_GENDER,select,"
              + "female{Reply to her.}"
              + "male{Reply to him.}"
              + "other{Reply to them.}}"
            + "}"
            + "other{"
              + "{TARGET_GENDER,select,"
              + "female{Reply to her.}"
              + "male{Reply to him.}"
              + "other{Reply to them.}}"
            + "}"
          + "}', "
          + "('USER_GENDER', 'TARGET_GENDER')), "
        + "{"
        + "'USER_GENDER': opt_data.get('userGender'), "
        + "'TARGET_GENDER': opt_data.get('targetGender')"
        + "})";

    assertThatSoyExpr(soyCode).compilesTo(new PyExpr(expectedPyCode, Integer.MAX_VALUE));
  }

  public void testMsgWithPluralWithGender() {
    String soyCode =
        "{msg genders=\"$people[0]?.gender, $people[1]?.gender\" desc=\"plural with offsets\"}\n"
        + "  {plural length($people)}\n"
        + "    {case 1}{$people[0].name} is attending\n"
        + "    {case 2}{$people[0].name} and {$people[1]?.name} are attending\n"
        + "    {case 3}{$people[0].name}, {$people[1]?.name}, and 1 other are attending\n"
        + "    {default}{$people[0].name}, {$people[1]?.name}, and length($people) others\n"
        + "  {/plural}\n"
        + "{/msg}\n";

    String expectedPyCode = "translator_impl.render_icu"
        + "(translator_impl.prepare_icu"
          + "(###, "
            + "'{PEOPLE_0_GENDER,select,"
              + "female{{PEOPLE_1_GENDER,select,"
                + "female{{NUM,plural,"
                  + "=1{{NAME_1} is attending}"
                  + "=2{{NAME_1} and {NAME_2} are attending}"
                  + "=3{{NAME_1}, {NAME_2}, and 1 other are attending}"
                  + "other{{NAME_1}, {NAME_2}, and length($people) others}}"
                + "}"
                + "male{{NUM,plural,"
                  + "=1{{NAME_1} is attending}"
                  + "=2{{NAME_1} and {NAME_2} are attending}"
                  + "=3{{NAME_1}, {NAME_2}, and 1 other are attending}"
                  + "other{{NAME_1}, {NAME_2}, and length($people) others}}"
                + "}"
                + "other{{NUM,plural,"
                  + "=1{{NAME_1} is attending}"
                  + "=2{{NAME_1} and {NAME_2} are attending}"
                  + "=3{{NAME_1}, {NAME_2}, and 1 other are attending}"
                  + "other{{NAME_1}, {NAME_2}, and length($people) others}}}"
                + "}"
              + "}"
              + "male{{PEOPLE_1_GENDER,select,"
                + "female{{NUM,plural,"
                  + "=1{{NAME_1} is attending}"
                  + "=2{{NAME_1} and {NAME_2} are attending}"
                  + "=3{{NAME_1}, {NAME_2}, and 1 other are attending}"
                  + "other{{NAME_1}, {NAME_2}, and length($people) others}}"
                + "}"
                + "male{{NUM,plural,"
                  + "=1{{NAME_1} is attending}"
                  + "=2{{NAME_1} and {NAME_2} are attending}"
                  + "=3{{NAME_1}, {NAME_2}, and 1 other are attending}"
                  + "other{{NAME_1}, {NAME_2}, and length($people) others}}"
                + "}"
                + "other{{NUM,plural,"
                  + "=1{{NAME_1} is attending}"
                  + "=2{{NAME_1} and {NAME_2} are attending}"
                  + "=3{{NAME_1}, {NAME_2}, and 1 other are attending}"
                  + "other{{NAME_1}, {NAME_2}, and length($people) others}}}"
                + "}"
              + "}"
              + "other{{PEOPLE_1_GENDER,select,"
                + "female{{NUM,plural,"
                  + "=1{{NAME_1} is attending}"
                  + "=2{{NAME_1} and {NAME_2} are attending}"
                  + "=3{{NAME_1}, {NAME_2}, and 1 other are attending}"
                  + "other{{NAME_1}, {NAME_2}, and length($people) others}}"
                + "}"
                + "male{{NUM,plural,"
                  + "=1{{NAME_1} is attending}"
                  + "=2{{NAME_1} and {NAME_2} are attending}"
                  + "=3{{NAME_1}, {NAME_2}, and 1 other are attending}"
                  + "other{{NAME_1}, {NAME_2}, and length($people) others}}"
                + "}"
                + "other{{NUM,plural,"
                  + "=1{{NAME_1} is attending}"
                  + "=2{{NAME_1} and {NAME_2} are attending}"
                  + "=3{{NAME_1}, {NAME_2}, and 1 other are attending}"
                  + "other{{NAME_1}, {NAME_2}, and length($people) others}}}"
                + "}"
              + "}"
            + "}', "
            + "('PEOPLE_0_GENDER', 'PEOPLE_1_GENDER', 'NUM', 'NAME_1', 'NAME_2')), "
          + "{"
            + "'PEOPLE_0_GENDER': None if opt_data.get('people')[0] is None "
              + "else opt_data.get('people')[0].get('gender'), "
            + "'PEOPLE_1_GENDER': None if opt_data.get('people')[1] is None "
              + "else opt_data.get('people')[1].get('gender'), "
            + "'NUM': len(opt_data.get('people')), "
            + "'NAME_1': str(opt_data.get('people')[0].get('name')), "
            + "'NAME_2': str(None if opt_data.get('people')[1] is None "
              + "else opt_data.get('people')[1].get('name'))"
          + "}"
        + ")";

    assertThatSoyExpr(soyCode).compilesTo(new PyExpr(expectedPyCode, Integer.MAX_VALUE));
  }
}
