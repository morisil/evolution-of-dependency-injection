/*
 * evolution-of-dependency-injection - how dependency injection in Java evolved
 * up to Google Guice container
 *
 * Copyright (C) 2018  Kazimierz Pogoda
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.xemantic.demo.inject;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

import java.lang.reflect.Field;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.Context;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;

/**
 * Dependency Injection, do we need this? Some theory is here:
 * <p>
 * http://en.wikipedia.org/wiki/Dependency_injection
 * http://martinfowler.com/articles/injection.html
 */
public class GuiceAndDependencyInjectionTest {


  /**
   * This test shows the intention of injecting one
   * component into another. It also shows that this
   * attempt fails. The idea behind DI containers is to
   * inject components into another components with
   * minimal effort.
   */
  @Test
  public void shouldStartDealingWithComponents() {
    // given
    class FooComponent {
    }
    class BarComponent {
      private FooComponent foo;
    }

    // when
    BarComponent bar = new BarComponent();

    // then
    assertThat(bar.foo, is(nullValue())); // how to effectively inject foo into bar?
  }


  /**
   * Old technique of using JNDI to decouple interface with
   * implementation.
   * <p>
   * http://java.sun.com/blueprints/corej2eepatterns/Patterns/ServiceLocator.html
   */
  @Test
  public void shouldProvideFooToBar() throws NamingException {
    // given
    class Foo {
    }
    class Bar {
      private final Foo foo;

      Bar(Context context) throws NamingException {
        foo = (Foo) context.lookup("someprotocol:foo");
      }
    }
    Context context = mock(Context.class);
    given(context.lookup("someprotocol:foo")).willReturn(new Foo());

    // when
    Bar bar = new Bar(context);

    // then
    assertThat(bar.foo, is(not(nullValue())));
  }
  /*
   * Note: this kind of "pull" approach, is still available
   * in containers like spring and guice. It was the default
   * modus operandi of Avalon based DI containers.
   *
   * http://avalon.apache.org/
   *
   * DI conclusion: it should be possible to decouple
   * the interface and the implementation, and configure
   * component bindings and properties in declarative way.
   */


  /* Note: singleton example requires static class to declare static field. */
  private static class FooSingleton {
    private static final FooSingleton INSTANCE = new FooSingleton();

    private FooSingleton() {
    }

    public static FooSingleton getInstance() {
      return INSTANCE;
    }
  }

  /**
   * Obtaining foo component as singleton. Advantage of
   * the singleton: it can be used anywhere in the application.
   * <p>
   * http://en.wikipedia.org/wiki/Singleton_pattern
   */
  @Test
  public void shouldBeAbleToObtainFooFromBar() {
    // given
    class Bar {
      private final FooSingleton foo;

      public Bar() {
        foo = FooSingleton.getInstance();
      }
    }

    // when
    Bar bar = new Bar();

    // then
    assertThat(bar.foo, is(not(nullValue())));
  }
  /*
   * Singleton is often considered antipattern. It introduces
   * global state into application and thus it is hard
   * to test, hard to mock, hard to configure, etc. Despite several
   * limitations it is still often used in standard JRE API
   * (e.g. Toolkit.getDefaultToolkit()), JSF, spring, etc.
   *
   * DI conclusion: 'new' operator is an evil, the container
   * should decide how instances of the components are created.
   * This is the same principle which founds the factory pattern.
   *
   * It should be possible to define singleton as a kind of
   * container specific scope (sometimes called "application scope")
   * Components configured with singleton scope should be
   * instantiated and configured by DI container, but only
   * one instance per container should be ever created.
   */












  /*
   * Maybe the POJO way is better. Spring, PicoContainer
   */


  /**
   * Setter injection.
   */
  @Test
  public void shouldInjectFooIntoBar() {
    // given
    class Foo {
    }
    class Bar {
      private Foo foo;

      public void setFoo(Foo foo) {
        this.foo = foo;
      }
    }

    // when
    Foo foo = new Foo();
    Bar bar = new Bar();
    bar.setFoo(foo);

    // then
    assertThat(bar.foo, is(not(nullValue())));
  }
  /*
   * In case of setter injection, the dependency is considered
   * as a bean property. It is an assumption which founds the
   * Spring DI container, although Spring is not limited to only
   * this kind of injection. With setter injection it is very
   * easy to solve the problem of circular dependencies as
   * components are instantiated first, and then interconnected
   * by the container via their setters.
   *
   * DI conclusion: container should analyze component (bean)
   * via reflection in order to determine how to inject dependencies.
   */


  /**
   * Constructor injection.
   */
  @Test
  public void shouldInjectFooIntoBarViaConstructor() {
    // given
    class Foo {
    }
    class Bar {
      private final Foo foo;

      public Bar(Foo foo) {
        this.foo = foo;
      }
    }

    // when
    Foo foo = new Foo();
    Bar bar = new Bar(foo);

    // then
    assertThat(bar.foo, is(not(nullValue())));
  }
  /*
   * Constructor based DI has several advantages.
   * All the dependencies required by the component
   * are defined in the constructor. After
   * invoking constructor the component is ready to use.
   * It is also the most natural way of instantiating
   * components "by hand" (without container), for example
   * in the unit tests. Components injected via constructor
   * can be also declared final. The disadvantages include:
   * big list of constructor parameters if there are
   * many dependencies, and problems with handling circular
   * dependencies (which is solved with cglib proxy in Guice).
   *
   * The constructor injection is preferred in Guice and
   * PicoContainer, although these containers can also
   * perform other kinds of injections.
   *
   * DI conclusion: users benefit from containers supporting
   * many types of injection.
   */


  /**
   * Field injection.
   */
  @Test
  public void shouldInjectFooIntoBarViaReflection() throws Exception {
    // given
    class Foo {
    }
    class Bar {
      private Foo foo;
    }

    // when
    Foo foo = new Foo();
    Bar bar = new Bar();
    Field field = Bar.class.getDeclaredField("foo");
    field.setAccessible(true);
    field.set(bar, foo);

    // then
    assertThat(bar.foo, is(not(nullValue())));
  }
  /*
   * There are situations where field injection is very
   * useful. For example in unit tests or seam actions.
   */


  /* Note: Guice example requires static class as components */
  static class Foo {
  }

  static class Bar {
    private final Foo foo;

    @Inject
    public Bar(Foo foo) {
      this.foo = foo;
    }
  }

  @Test
  public void shouldInjectFooIntoBarWithGuice() {
    // given Foo and Bar components

    // when
    Injector injector = Guice.createInjector();
    Bar bar = injector.getInstance(Bar.class);

    // then
    assertThat(bar.foo, is(not(nullValue())));
  }


  static class Buzz {
    @Inject
    private Foo foo;
    @Inject
    private Bar bar;
  }

  @Test
  public void shouldInjectFooIntoBarAndBuzz() {
    // given Foo, Bar and Buzz components

    // when
    Injector injector = Guice.createInjector();
    Buzz buzz = injector.getInstance(Buzz.class);

    // then
    assertThat(buzz.foo, is(not(nullValue())));
    assertThat(buzz.bar, is(not(nullValue())));
    assertThat(buzz.bar.foo, is(not(nullValue())));
    assertThat(buzz.bar.foo == buzz.foo, is(false));
  }

  @Test
  public void shouldInjectFooIntoBarAndBuzzAndFooShouldBeInSingletonScope() {
    // given Foo, Bar and Buzz components

    // when
    Injector injector = Guice.createInjector(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(Foo.class).in(Singleton.class);
          }
        });
    Buzz buzz = injector.getInstance(Buzz.class);

    // then
    assertThat(buzz.foo, is(not(nullValue())));
    assertThat(buzz.bar, is(not(nullValue())));
    assertThat(buzz.bar.foo, is(not(nullValue())));
    assertThat(buzz.bar.foo == buzz.foo, is(true));
  }


  public interface BuzzInspector {
    String inspect();
  }

  @Singleton
  static class DefaultBuzzInspector implements BuzzInspector {
    private Buzz buzz;

    @Inject
    public void setBuzz(Buzz buzz) {
      this.buzz = buzz;
    }

    @Override
    public String inspect() {
      return buzz.toString();
    }
  }

  @Singleton
  static class ExtendedBuzzInspector implements BuzzInspector {
    private Buzz buzz;

    @Inject
    public void setBuzz(Buzz buzz) {
      this.buzz = buzz;
    }

    @Override
    public String inspect() {
      return buzz.toString() + "Extended";
    }
  }

  @Test
  public void shouldBindInterfaceWithImplementation() {
    // given Foo, Bar, Buzz and BuzzInspector components

    // when
    Injector injector = Guice.createInjector(
        new AbstractModule() {
          protected void configure() {
            bind(Buzz.class).in(Singleton.class);
            bind(BuzzInspector.class)
                .annotatedWith(Names.named("default"))
                .to(DefaultBuzzInspector.class);
            bind(BuzzInspector.class)
                .annotatedWith(Names.named("extended"))
                .to(ExtendedBuzzInspector.class);
          }
        });
    MegaInspector inspector = injector.getInstance(MegaInspector.class);
    //.String result = inspector.inspect();

    // then
    //assertThat(result, is(not(nullValue())));
    //assertThat(inspector instanceof DefaultBuzzInspector, is(true));
  }


  static class MegaInspector {

    @Inject
    public MegaInspector(
        @Named("default")
            BuzzInspector defInspector,
        @Named("extended")
            BuzzInspector extInspector) {

      System.out.println(defInspector.inspect());
      System.out.println(extInspector.inspect());
    }
  }


  public interface Yin {
    Yang getYang();
  }

  public interface Yang {
    Yin getYin();
  }

  @Singleton
  static class DefaultYin implements Yin {
    private Yang yang;

    @Inject
    public DefaultYin(Yang yang) {
      this.yang = yang;
    }

    @Override
    public Yang getYang() {
      return yang;
    }
  }

  @Singleton
  static class DefaultYang implements Yang {
    private Yin yin;

    @Inject
    public DefaultYang(Yin yin) {
      this.yin = yin;
    }

    @Override
    public Yin getYin() {
      return yin;
    }
  }

  static class TaoismModule extends AbstractModule {
    @Override
    public void configure() {
      bind(Yin.class).to(DefaultYin.class);
      bind(Yang.class).to(DefaultYang.class);
    }
  }

  @Test
  public void shouldInjectCircularDependencies() {
    // given Yin and Yang

    // when
    Injector injector = Guice.createInjector(new TaoismModule());
    Yin yin = injector.getInstance(Yin.class);
    Yang yang = injector.getInstance(Yang.class);

    // then
    assertThat(yin.getYang(), is(not(nullValue())));
    assertThat(yang.getYin(), is(not(nullValue())));
    assertThat(yang.getYin() == yin, is(false));
    assertThat(yin.getYang() == yang, is(true));
  }


  /**
   * Aspect Oriented Programming
   */
  @Test
  public void shouldCreateInterceptor() {
    // given Yin and Yang
    //final Logger logger = Logger.getLogger("test-logger");
    final MethodInterceptor interceptor = new MethodInterceptor() {

      @Override
      public Object invoke(MethodInvocation invocation) throws Throwable {
        String name = invocation.getThis().getClass().getName();
        String method = invocation.getMethod().getName();
        System.out.println("entering " + name + "#" + method);
        try {
          return invocation.proceed();
        } finally {
          System.out.println("exiting " + name + "#" + method);
        }
      }
    };

    // when
    Injector injector = Guice.createInjector(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(BuzzInspector.class).to(DefaultBuzzInspector.class);
            bindInterceptor(
                Matchers.subclassesOf(BuzzInspector.class),
                Matchers.any(),
                interceptor);
          }
        });
    BuzzInspector inspector = injector.getInstance(BuzzInspector.class);
    inspector.inspect();

    // then
    //assertThat(buzz, is(not(nullValue())));
  }

}
