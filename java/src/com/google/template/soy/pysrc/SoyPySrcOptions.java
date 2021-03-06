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

package com.google.template.soy.pysrc;


/**
 * Compilation options for the Python backend.
 *
 */
public final class SoyPySrcOptions implements Cloneable {
  /** The full module and fn path to a runtime library for determining global directionality. */
  private final String bidiIsRtlFn;

  /** The base module path for loading the runtime modules. */
  private final String runtimePath;

  /** The absolute translation module name used in Python runtime. */
  private final String translationPyModuleName;

  public SoyPySrcOptions(String runtimePath, String bidiIsRtlFn, String translationPyModuleName) {
    this.runtimePath = runtimePath;
    this.bidiIsRtlFn = bidiIsRtlFn;
    this.translationPyModuleName = translationPyModuleName;
  }

  private SoyPySrcOptions(SoyPySrcOptions orig) {
    this.runtimePath = orig.runtimePath;
    this.bidiIsRtlFn = orig.bidiIsRtlFn;
    this.translationPyModuleName = orig.translationPyModuleName;
  }

  public String getBidiIsRtlFn() {
    return bidiIsRtlFn;
  }

  public String getRuntimePath() {
    return runtimePath;
  }

  public String getTranslationPyModuleName() {
    return translationPyModuleName;
  }

  @Override public final SoyPySrcOptions clone() {
    return new SoyPySrcOptions(this);
  }

}
