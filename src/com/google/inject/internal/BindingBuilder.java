/**
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject.internal;

import com.google.inject.Binder;
import com.google.inject.ConfigurationException;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.AnnotatedElementBuilder;
import static com.google.inject.internal.Preconditions.checkNotNull;
import com.google.inject.spi.Element;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.Message;
import com.google.inject.spi.PrivateElements;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * Bind a non-constant key.
 *
 * @author jessewilson@google.com (Jesse Wilson)
 */
public class BindingBuilder<T> extends AbstractBindingBuilder<T>
    implements AnnotatedBindingBuilder<T> {

  public BindingBuilder(Binder binder, List<Element> elements, Object source, Key<T> key) {
    super(binder, elements, source, key);
  }

  public BindingBuilder<T> annotatedWith(Class<? extends Annotation> annotationType) {
    annotatedWithInternal(annotationType);
    return this;
  }

  public BindingBuilder<T> annotatedWith(Annotation annotation) {
    annotatedWithInternal(annotation);
    return this;
  }

  public BindingBuilder<T> to(Class<? extends T> implementation) {
    return to(Key.get(implementation));
  }

  public BindingBuilder<T> to(TypeLiteral<? extends T> implementation) {
    return to(Key.get(implementation));
  }

  public BindingBuilder<T> to(Key<? extends T> linkedKey) {
    checkNotNull(linkedKey, "linkedKey");
    checkNotTargetted();
    BindingImpl<T> base = getBinding();
    setBinding(new LinkedBindingImpl<T>(
        base.getSource(), base.getKey(), base.getScoping(), linkedKey));
    return this;
  }

  public void toInstance(T instance) {
    checkNotTargetted();

    // lookup the injection points, adding any errors to the binder's errors list
    Set<InjectionPoint> injectionPoints;
    if (instance != null) {
      try {
        injectionPoints = InjectionPoint.forInstanceMethodsAndFields(instance.getClass());
      } catch (ConfigurationException e) {
        for (Message message : e.getErrorMessages()) {
          binder.addError(message);
        }
        injectionPoints = e.getPartialValue();
      }
    } else {
      binder.addError(BINDING_TO_NULL);
      injectionPoints = ImmutableSet.of();
    }

    BindingImpl<T> base = getBinding();
    setBinding(new InstanceBindingImpl<T>(
        base.getSource(), base.getKey(), base.getScoping(), injectionPoints, instance));
  }

  public BindingBuilder<T> toProvider(Provider<? extends T> provider) {
    checkNotNull(provider, "provider");
    checkNotTargetted();

    // lookup the injection points, adding any errors to the binder's errors list
    Set<InjectionPoint> injectionPoints;
    try {
      injectionPoints = InjectionPoint.forInstanceMethodsAndFields(provider.getClass());
    } catch (ConfigurationException e) {
      for (Message message : e.getErrorMessages()) {
        binder.addError(message);
      }
      injectionPoints = e.getPartialValue();
    }

    BindingImpl<T> base = getBinding();
    setBinding(new ProviderInstanceBindingImpl<T>(
        base.getSource(), base.getKey(), base.getScoping(), injectionPoints, provider));
    return this;
  }

  public BindingBuilder<T> toProvider(Class<? extends Provider<? extends T>> providerType) {
    return toProvider(Key.get(providerType));
  }

  public BindingBuilder<T> toProvider(Key<? extends Provider<? extends T>> providerKey) {
    checkNotNull(providerKey, "providerKey");
    checkNotTargetted();

    BindingImpl<T> base = getBinding();
    setBinding(new LinkedProviderBindingImpl<T>(
        base.getSource(), base.getKey(), base.getScoping(), providerKey));
    return this;
  }

  public ExposureBuilder<T> usingKeyFrom(PrivateElements privateElements) {
    checkNotTargetted();
    checkNotScoped();

    BindingImpl<T> base = getBinding();
    ExposedBindingImpl<T> exposedBinding = new ExposedBindingImpl<T>(
        base.getSource(), base.getKey(), base.getScoping(), privateElements);
    setBinding(exposedBinding);

    return new ExposureBuilder<T>(this, exposedBinding);
  }

  @Override public String toString() {
    return "BindingBuilder<" + getBinding().getKey().getTypeLiteral() + ">";
  }

  /**
   * For private binder's expose() method.
   */
  public static class ExposureBuilder<T> implements AnnotatedElementBuilder {
    private final BindingBuilder<T> bindingBuilder;
    private BindingImpl<T> binding;

    public ExposureBuilder(BindingBuilder<T> bindingBuilder, ExposedBindingImpl<T> binding) {
      this.binding = binding;
      this.bindingBuilder = bindingBuilder;
    }

    public void annotatedWith(Class<? extends Annotation> annotationType) {
      binding = bindingBuilder.annotatedWithInternal(annotationType);
    }

    public void annotatedWith(Annotation annotation) {
      binding = bindingBuilder.annotatedWithInternal(annotation);
    }

    public Key<?> getKey() {
      return binding.getKey();
    }

    @Override public String toString() {
      return "AnnotatedElementBuilder";
    }
  }
}