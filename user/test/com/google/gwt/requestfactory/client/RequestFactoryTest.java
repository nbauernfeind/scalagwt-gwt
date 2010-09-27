/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.requestfactory.client;

import com.google.gwt.requestfactory.client.impl.ProxyImpl;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyChange;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.SimpleBarProxy;
import com.google.gwt.requestfactory.shared.SimpleEnum;
import com.google.gwt.requestfactory.shared.SimpleFooProxy;
import com.google.gwt.requestfactory.shared.Violation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link com.google.gwt.requestfactory.shared.RequestFactory}.
 */
public class RequestFactoryTest extends RequestFactoryTestBase {
  /*
   * DO NOT USE finishTest(). Instead, call finishTestAndReset();
   */

  class FooReciever extends Receiver<SimpleFooProxy> {
    private SimpleFooProxy mutableFoo;
    private Request<SimpleFooProxy> persistRequest;
    private String expectedException;

    public FooReciever(SimpleFooProxy mutableFoo,
        Request<SimpleFooProxy> persistRequest, String exception) {
      this.mutableFoo = mutableFoo;
      this.persistRequest = persistRequest;
      this.expectedException = exception;
    }

    @Override
    public void onFailure(ServerFailure error) {
      assertEquals(expectedException, error.getExceptionType());
      if (expectedException.length() > 0) {
        assertFalse(error.getStackTraceString().length() == 0);
        assertEquals("THIS EXCEPTION IS EXPECTED BY A TEST", error.getMessage());
      } else {
        assertEquals("", error.getStackTraceString());
        assertEquals("Server Error: THIS EXCEPTION IS EXPECTED BY A TEST",
            error.getMessage());
      }

      // Now show that we can fix the error and try again with the same
      // request

      mutableFoo.setPleaseCrash(24); // Only 42 and 43 crash
      persistRequest.fire(new Receiver<SimpleFooProxy>() {
        @Override
        public void onSuccess(SimpleFooProxy response) {
          finishTestAndReset();
        }
      });
    }

    @Override
    public void onSuccess(SimpleFooProxy response) {
      fail("Failure expected but onSuccess() was called");
    }

    @Override
    public void onViolation(Set<Violation> errors) {
      fail("Failure expected but onViolation() was called");
    }
  }

  class NullReceiver extends Receiver<Object> {
    @Override
    public void onSuccess(Object response) {
      assertNull(response);
      finishTestAndReset();
    }
  }

  private class FailFixAndRefire<T> extends Receiver<T> {

    private final SimpleFooProxy proxy;
    private final Request<T> request;
    private boolean voidReturnExpected;

    FailFixAndRefire(SimpleFooProxy proxy, Request<T> request) {
      this.proxy = request.edit(proxy);
      this.request = request;
    }

    @Override
    public void onSuccess(T response) {
      /*
       * Make sure your class path includes:
       * 
       * tools/lib/apache/log4j/log4j-1.2.16.jar
       * tools/lib/hibernate/validator/hibernate-validator-4.1.0.Final.jar
       * tools/lib/slf4j/slf4j-api/slf4j-api-1.6.1.jar
       * tools/lib/slf4j/slf4j-log4j12/slf4j-log4j12-1.6.1.jar
       */
      fail("Violations expected (you might be missing some jars, "
          + "see the comment above this line)");
    }

    @Override
    public void onViolation(Set<Violation> errors) {

      // size violation expected

      assertEquals(1, errors.size());
      Violation error = errors.iterator().next();
      assertEquals("userName", error.getPath());
      assertEquals("size must be between 3 and 30", error.getMessage());
      assertEquals(proxy.stableId(), error.getProxyId());

      // Now re-used the request to fix the edit

      proxy.setUserName("long enough");
      request.fire(new Receiver<T>() {
        @Override
        public void onSuccess(T response) {
          if (voidReturnExpected) {
            assertNull(response);
          } else {
            assertEquals(proxy.stableId(),
                ((SimpleFooProxy) response).stableId());
          }
          finishTestAndReset();
        }
      });
    }

    void doTest() {
      proxy.setUserName("a"); // too short
      request.fire(this);
    }

    void doVoidTest() {
      voidReturnExpected = true;
      doTest();
    }
  }

  public <T extends EntityProxy> void assertContains(Collection<T> col, T value) {
    for (T x : col) {
      if (x.stableId().equals(value.stableId())) {
        return;
      }
    }
    assertTrue(("Value " + value + " not found in collection ")
        + col.toString(), false);
  }

  public <T extends EntityProxy> void assertNotContains(Collection<T> col,
      T value) {
    for (T x : col) {
      assertNotSame(x.stableId(), value.stableId());
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }

  public void testClassToken() {
    String token = req.getHistoryToken(SimpleFooProxy.class);
    assertEquals(SimpleFooProxy.class, req.getProxyClass(token));

    SimpleFooProxy foo = req.create(SimpleFooProxy.class);
    assertEquals(SimpleFooProxy.class, foo.stableId().getProxyClass());
  }

  public void testDummyCreate() {
    delayTestFinish(5000);

    final SimpleFooEventHandler<SimpleFooProxy> handler = new SimpleFooEventHandler<SimpleFooProxy>();
    EntityProxyChange.registerForProxyType(req.getEventBus(),
        SimpleFooProxy.class, handler);

    final SimpleFooProxy foo = req.create(SimpleFooProxy.class);
    Object futureId = foo.getId();
    assertEquals(futureId, foo.getId());
    assertTrue(((ProxyImpl) foo).unpersisted());
    Request<SimpleFooProxy> fooReq = req.simpleFooRequest().persistAndReturnSelf(
        foo);
    fooReq.fire(new Receiver<SimpleFooProxy>() {

      @Override
      public void onSuccess(final SimpleFooProxy returned) {
        Object futureId = foo.getId();
        assertEquals(futureId, foo.getId());
        assertTrue(((ProxyImpl) foo).unpersisted());
        assertEquals(1, handler.persistEventCount);
        assertEquals(1, handler.updateEventCount);
        assertEquals(2, handler.totalEventCount);

        checkStableIdEquals(foo, returned);
        finishTestAndReset();
      }
    });
  }

  public void testDummyCreateBar() {
    delayTestFinish(5000);

    final SimpleBarProxy foo = req.create(SimpleBarProxy.class);
    Object futureId = foo.getId();
    assertEquals(futureId, foo.getId());
    assertTrue(((ProxyImpl) foo).unpersisted());
    Request<SimpleBarProxy> fooReq = req.simpleBarRequest().persistAndReturnSelf(
        foo);
    fooReq.fire(new Receiver<SimpleBarProxy>() {

      @Override
      public void onSuccess(final SimpleBarProxy returned) {
        Object futureId = foo.getId();
        assertEquals(futureId, foo.getId());
        assertTrue(((ProxyImpl) foo).unpersisted());

        checkStableIdEquals(foo, returned);
        finishTestAndReset();
      }
    });
  }

  public void testDummyCreateList() {
    delayTestFinish(500000);

    final SimpleBarProxy bar = req.create(SimpleBarProxy.class);
    Object futureId = bar.getId();
    assertEquals(futureId, bar.getId());
    assertTrue(((ProxyImpl) bar).unpersisted());
    Request<SimpleBarProxy> fooReq = req.simpleBarRequest().returnFirst(
        Collections.singletonList(bar));
    fooReq.fire(new Receiver<SimpleBarProxy>() {

      @Override
      public void onSuccess(final SimpleBarProxy returned) {
        Object futureId = bar.getId();
        assertEquals(futureId, bar.getId());
        assertTrue(((ProxyImpl) bar).unpersisted());
        assertFalse(((ProxyImpl) returned).unpersisted());

        checkStableIdEquals(bar, returned);
        finishTestAndReset();
      }
    });
  }

  public void testFetchEntity() {
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response) {
            assertEquals(42, (int) response.getIntId());
            assertEquals("GWT", response.getUserName());
            assertEquals(8L, (long) response.getLongField());
            assertEquals(com.google.gwt.requestfactory.shared.SimpleEnum.FOO,
                response.getEnumField());
            assertEquals(null, response.getBarField());
            finishTestAndReset();
          }
        });
  }

  public void testFetchEntityWithRelation() {
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).with("barField").fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response) {
            assertEquals(42, (int) response.getIntId());
            assertEquals("GWT", response.getUserName());
            assertEquals(8L, (long) response.getLongField());
            assertEquals(com.google.gwt.requestfactory.shared.SimpleEnum.FOO,
                response.getEnumField());
            assertNotNull(response.getBarField());
            finishTestAndReset();
          }
        });
  }

  public void testFetchList() {
    delayTestFinish(5000);
    req.simpleFooRequest().findAll().fire(new Receiver<List<SimpleFooProxy>>() {
      @Override
      public void onSuccess(List<SimpleFooProxy> responseList) {
        SimpleFooProxy response = responseList.get(0);
        assertEquals(42, (int) response.getIntId());
        assertEquals("GWT", response.getUserName());
        assertEquals(8L, (long) response.getLongField());
        assertEquals(com.google.gwt.requestfactory.shared.SimpleEnum.FOO,
            response.getEnumField());
        finishTestAndReset();
      }
    });
  }

  public void testFetchSet() {
    delayTestFinish(5000);
    req.simpleBarRequest().findAsSet().fire(
        new Receiver<Set<SimpleBarProxy>>() {
          @Override
          public void onSuccess(Set<SimpleBarProxy> response) {
            assertEquals(2, response.size());
            finishTestAndReset();
          }
        });
  }

  public void testGetEventBus() {
    assertEquals(eventBus, req.getEventBus());
  }

  public void testGetListLongId() {
    delayTestFinish(5000);

    // Long ids
    req.simpleFooRequest().findAll().with("barField.userName").fire(
        new Receiver<List<SimpleFooProxy>>() {
          @Override
          public void onSuccess(List<SimpleFooProxy> response) {
            assertEquals(1, response.size());
            for (SimpleFooProxy foo : response) {
              assertNotNull(foo.stableId());
              assertEquals("FOO", foo.getBarField().getUserName());
            }
            finishTestAndReset();
          }
        });
  }

  public void testGetListStringId() {
    delayTestFinish(5000);

    // String ids
    req.simpleBarRequest().findAll().fire(new Receiver<List<SimpleBarProxy>>() {
      @Override
      public void onSuccess(List<SimpleBarProxy> response) {
        assertEquals(2, response.size());
        for (SimpleBarProxy bar : response) {
          assertNotNull(bar.stableId());
          finishTestAndReset();
        }
        finishTestAndReset();
      }
    });
  }

  public void testHistoryToken() {
    delayTestFinish(5000);
    final SimpleBarProxy foo = req.create(SimpleBarProxy.class);
    final EntityProxyId<SimpleBarProxy> futureId = foo.stableId();
    final String futureToken = req.getHistoryToken(futureId);

    // Check that a newly-created object's token can be found
    assertEquals(futureId, req.getProxyId(futureToken));
    assertEquals(futureId.getProxyClass(), req.getProxyClass(futureToken));

    Request<SimpleBarProxy> fooReq = req.simpleBarRequest().persistAndReturnSelf(
        foo);
    fooReq.fire(new Receiver<SimpleBarProxy>() {
      @Override
      public void onSuccess(final SimpleBarProxy returned) {
        EntityProxyId<SimpleBarProxy> persistedId = returned.stableId();
        String persistedToken = req.getHistoryToken(returned.stableId());

        // Expect variations after persist
        assertFalse(futureToken.equals(persistedToken));

        // Make sure the token is stable after persist using the future id
        assertEquals(persistedToken, req.getHistoryToken(futureId));

        // Check that the persisted object can be found with future token
        assertEquals(futureId, req.getProxyId(futureToken));
        assertEquals(futureId, req.getProxyId(persistedToken));
        assertEquals(futureId.getProxyClass(),
            req.getProxyClass(persistedToken));

        assertEquals(persistedId, req.getProxyId(futureToken));
        assertEquals(persistedId, req.getProxyId(persistedToken));
        assertEquals(persistedId.getProxyClass(),
            req.getProxyClass(futureToken));

        finishTestAndReset();
      }
    });
  }

  /**
   * Ensures that a service method can respond with a null value.
   */
  public void testNullListResult() {
    delayTestFinish(5000);
    req.simpleFooRequest().returnNullList().fire(new NullReceiver());
  }

  /**
   * Ensures that a service method can respond with a null value.
   */
  public void testNullEntityProxyResult() {
    delayTestFinish(5000);
    req.simpleFooRequest().returnNullSimpleFoo().fire(new NullReceiver());
  }

  /**
   * Ensures that a service method can respond with a null value.
   */
  public void testNullStringResult() {
    delayTestFinish(5000);
    req.simpleFooRequest().returnNullString().fire(new NullReceiver());
  }

  /*
   * tests that (a) any method can have a side effect that is handled correctly.
   * (b) instance methods are handled correctly and (c) a request cannot be
   * reused after a successful response is received. (Yet?)
   */
  public void testMethodWithSideEffects() {
    delayTestFinish(5000);

    final SimpleFooEventHandler<SimpleFooProxy> handler = new SimpleFooEventHandler<SimpleFooProxy>();
    EntityProxyChange.registerForProxyType(req.getEventBus(),
        SimpleFooProxy.class, handler);

    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {

          @Override
          public void onSuccess(SimpleFooProxy newFoo) {
            assertEquals(1, handler.updateEventCount);
            assertEquals(1, handler.totalEventCount);
            final Request<Long> mutateRequest = req.simpleFooRequest().countSimpleFooWithUserNameSideEffect(
                newFoo);
            newFoo = mutateRequest.edit(newFoo);
            newFoo.setUserName("Ray");
            mutateRequest.fire(new Receiver<Long>() {
              @Override
              public void onSuccess(Long response) {
                assertCannotFire(mutateRequest);
                assertEquals(new Long(1L), response);
                assertEquals(2, handler.updateEventCount);
                assertEquals(2, handler.totalEventCount);

                // confirm that the instance method did have the desired
                // sideEffect.
                req.simpleFooRequest().findSimpleFooById(999L).fire(
                    new Receiver<SimpleFooProxy>() {
                      @Override
                      public void onSuccess(SimpleFooProxy finalFoo) {
                        assertEquals("Ray", finalFoo.getUserName());
                        assertEquals(3, handler.updateEventCount);
                        assertEquals(3, handler.totalEventCount);
                        finishTestAndReset();
                      }
                    });
              }

            });

            try {
              newFoo.setUserName("Barney");
              fail();
            } catch (IllegalStateException e) {
              /* pass, cannot change a request that is in flight */
            }
          }
        });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistExistingEntityExistingRelation() {
    delayTestFinish(5000);

    req.simpleBarRequest().findSimpleBarById("999L").fire(
        new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(final SimpleBarProxy barProxy) {
            req.simpleFooRequest().findSimpleFooById(999L).fire(
                new Receiver<SimpleFooProxy>() {
                  @Override
                  public void onSuccess(SimpleFooProxy fooProxy) {
                    Request<Void> updReq = req.simpleFooRequest().persist(
                        fooProxy);
                    fooProxy = updReq.edit(fooProxy);
                    fooProxy.setBarField(barProxy);
                    updReq.fire(new Receiver<Void>() {
                      @Override
                      public void onSuccess(Void response) {

                        finishTestAndReset();
                      }
                    });
                  }
                });
          }
        });
  }

  /*
   * Find Entity Create Entity2 Relate Entity2 to Entity Persist Entity
   */
  public void testPersistExistingEntityNewRelation() {
    delayTestFinish(5000);
    // Make a new bar
    SimpleBarProxy makeABar = req.create(SimpleBarProxy.class);
    Request<SimpleBarProxy> persistRequest = req.simpleBarRequest().persistAndReturnSelf(
        makeABar);
    makeABar = persistRequest.edit(makeABar);
    makeABar.setUserName("Amit");

    persistRequest.fire(new Receiver<SimpleBarProxy>() {
      @Override
      public void onSuccess(final SimpleBarProxy persistedBar) {

        // It was made, now find a foo to assign it to
        req.simpleFooRequest().findSimpleFooById(999L).fire(
            new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy response) {

                // Found the foo, edit it
                Request<Void> fooReq = req.simpleFooRequest().persist(response);
                response = fooReq.edit(response);
                response.setBarField(persistedBar);
                fooReq.fire(new Receiver<Void>() {
                  @Override
                  public void onSuccess(Void response) {

                    // Foo was persisted, fetch it again check the goods
                    req.simpleFooRequest().findSimpleFooById(999L).with(
                        "barField.userName").fire(
                        new Receiver<SimpleFooProxy>() {

                          // Here it is
                          @Override
                          public void onSuccess(SimpleFooProxy finalFooProxy) {
                            assertEquals("Amit",
                                finalFooProxy.getBarField().getUserName());
                            finishTestAndReset();
                          }
                        });
                  }
                });
              }
            });
      }
    });
  }

  /*
   * Find Entity2 Create Entity, Persist Entity Relate Entity2 to Entity Persist
   * Entity
   */
  public void testPersistNewEntityExistingRelation() {
    delayTestFinish(5000);
    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);

    final Request<Void> fooReq = req.simpleFooRequest().persist(newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("Ray");

    final SimpleFooProxy finalFoo = newFoo;
    req.simpleBarRequest().findSimpleBarById("999L").fire(
        new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(SimpleBarProxy response) {
            finalFoo.setBarField(response);
            fooReq.fire(new Receiver<Void>() {
              @Override
              public void onSuccess(Void response) {
                req.simpleFooRequest().findSimpleFooById(999L).fire(
                    new Receiver<SimpleFooProxy>() {
                      @Override
                      public void onSuccess(SimpleFooProxy finalFooProxy) {
                        // newFoo hasn't been persisted, so userName is the old
                        // value.
                        assertEquals("GWT", finalFooProxy.getUserName());
                        finishTestAndReset();
                      }
                    });
              }
            });
          }
        });
  }

  /**
   * Ensure that a relationship can be set up between two newly-created objects.
   */
  public void testPersistFutureToFuture() {
    delayTestFinish(500000);
    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);
    final SimpleBarProxy newBar = req.create(SimpleBarProxy.class);

    Request<SimpleFooProxy> fooReq = req.simpleFooRequest().persistAndReturnSelf(
        newFoo).with("barField");
    newFoo = fooReq.edit(newFoo);
    newFoo.setBarField(newBar);

    fooReq.fire(new Receiver<SimpleFooProxy>() {
      @Override
      public void onSuccess(SimpleFooProxy response) {
        assertNotNull(response.getBarField());
        assertEquals(newBar.stableId(), response.getBarField().stableId());
        finishTestAndReset();
      }
    });
  }

  /*
   * Create Entity, Persist Entity Create Entity2, Perist Entity2 relate Entity2
   * to Entity Persist
   */
  public void testPersistNewEntityNewRelation() {
    delayTestFinish(5000);
    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);
    SimpleBarProxy newBar = req.create(SimpleBarProxy.class);

    final Request<SimpleFooProxy> fooReq = req.simpleFooRequest().persistAndReturnSelf(
        newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("Ray");

    final Request<SimpleBarProxy> barReq = req.simpleBarRequest().persistAndReturnSelf(
        newBar);
    newBar = barReq.edit(newBar);
    newBar.setUserName("Amit");

    fooReq.fire(new Receiver<SimpleFooProxy>() {
      @Override
      public void onSuccess(final SimpleFooProxy persistedFoo) {
        barReq.fire(new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(final SimpleBarProxy persistedBar) {
            assertEquals("Ray", persistedFoo.getUserName());
            final Request<Void> fooReq2 = req.simpleFooRequest().persist(
                persistedFoo);
            SimpleFooProxy editablePersistedFoo = fooReq2.edit(persistedFoo);
            editablePersistedFoo.setBarField(persistedBar);
            fooReq2.fire(new Receiver<Void>() {
              @Override
              public void onSuccess(Void response) {
                req.simpleFooRequest().findSimpleFooById(999L).with(
                    "barField.userName").fire(new Receiver<SimpleFooProxy>() {
                  @Override
                  public void onSuccess(SimpleFooProxy finalFooProxy) {
                    assertEquals("Amit",
                        finalFooProxy.getBarField().getUserName());
                    finishTestAndReset();
                  }
                });
              }
            });
          }
        });
      }
    });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistOneToManyExistingEntityExistingRelation() {
    delayTestFinish(5000);

    req.simpleBarRequest().findSimpleBarById("999L").fire(
        new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(final SimpleBarProxy barProxy) {
            req.simpleFooRequest().findSimpleFooById(999L).with(
                "oneToManyField").fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy fooProxy) {
                Request<SimpleFooProxy> updReq = req.simpleFooRequest().persistAndReturnSelf(
                    fooProxy).with("oneToManyField");
                fooProxy = updReq.edit(fooProxy);

                List<SimpleBarProxy> barProxyList = fooProxy.getOneToManyField();
                final int listCount = barProxyList.size();
                barProxyList.add(barProxy);
                updReq.fire(new Receiver<SimpleFooProxy>() {
                  @Override
                  public void onSuccess(SimpleFooProxy response) {
                    assertEquals(response.getOneToManyField().size(),
                        listCount + 1);
                    assertContains(response.getOneToManyField(), barProxy);
                    finishTestAndReset();
                  }
                });
              }
            });
          }
        });
  }

  public void testPersistRecursiveRelation() {
    delayTestFinish(5000);

    SimpleFooProxy rayFoo = req.create(SimpleFooProxy.class);
    final Request<SimpleFooProxy> persistRay = req.simpleFooRequest().persistAndReturnSelf(
        rayFoo);
    rayFoo = persistRay.edit(rayFoo);
    rayFoo.setUserName("Ray");
    rayFoo.setFooField(rayFoo);
    persistRay.fire(new Receiver<SimpleFooProxy>() {
      @Override
      public void onSuccess(final SimpleFooProxy persistedRay) {
        finishTestAndReset();
      }
    });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */

  public void testPersistRelation() {
    delayTestFinish(5000);

    SimpleFooProxy rayFoo = req.create(SimpleFooProxy.class);
    final Request<SimpleFooProxy> persistRay = req.simpleFooRequest().persistAndReturnSelf(
        rayFoo);
    rayFoo = persistRay.edit(rayFoo);
    rayFoo.setUserName("Ray");

    persistRay.fire(new Receiver<SimpleFooProxy>() {
      @Override
      public void onSuccess(final SimpleFooProxy persistedRay) {
        SimpleBarProxy amitBar = req.create(SimpleBarProxy.class);
        final Request<SimpleBarProxy> persistAmit = req.simpleBarRequest().persistAndReturnSelf(
            amitBar);
        amitBar = persistAmit.edit(amitBar);
        amitBar.setUserName("Amit");

        persistAmit.fire(new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(SimpleBarProxy persistedAmit) {

            final Request<SimpleFooProxy> persistRelationship = req.simpleFooRequest().persistAndReturnSelf(
                persistedRay).with("barField");
            SimpleFooProxy newRec = persistRelationship.edit(persistedRay);
            newRec.setBarField(persistedAmit);

            persistRelationship.fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy relatedRay) {
                assertEquals("Amit", relatedRay.getBarField().getUserName());
                finishTestAndReset();
              }
            });
          }
        });
      }
    });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */

  public void testPersistSelfOneToManyExistingEntityExistingRelation() {
    delayTestFinish(5000);

    req.simpleFooRequest().findSimpleFooById(999L).with("selfOneToManyField").fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy fooProxy) {
            Request<SimpleFooProxy> updReq = req.simpleFooRequest().persistAndReturnSelf(
                fooProxy).with("selfOneToManyField");
            fooProxy = updReq.edit(fooProxy);
            List<SimpleFooProxy> fooProxyList = fooProxy.getSelfOneToManyField();
            final int listCount = fooProxyList.size();
            fooProxyList.add(fooProxy);
            updReq.fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy response) {
                assertEquals(response.getSelfOneToManyField().size(),
                    listCount + 1);
                assertContains(response.getSelfOneToManyField(), response);
                finishTestAndReset();
              }
            });
          }
        });
  }

  public void testPersistValueList() {
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy fooProxy) {
            Request<SimpleFooProxy> updReq = req.simpleFooRequest().persistAndReturnSelf(
                fooProxy);
            fooProxy = updReq.edit(fooProxy);
            fooProxy.getNumberListField().add(100);
            updReq.fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy response) {
                assertTrue(response.getNumberListField().contains(100));
                finishTestAndReset();
              }
            });
          }
        });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistValueListNull() {
    delayTestFinish(500000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy fooProxy) {
            Request<SimpleFooProxy> updReq = req.simpleFooRequest().persistAndReturnSelf(
                fooProxy);
            fooProxy = updReq.edit(fooProxy);

            fooProxy.setNumberListField(null);
            updReq.fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy response) {
                List<Integer> list = response.getNumberListField();
                assertNull(list);
                finishTestAndReset();
              }
            });
          }
        });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistValueListRemove() {
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy fooProxy) {
            Request<SimpleFooProxy> updReq = req.simpleFooRequest().persistAndReturnSelf(
                fooProxy);
            fooProxy = updReq.edit(fooProxy);
            final int oldValue = fooProxy.getNumberListField().remove(0);
            updReq.fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy response) {
                assertFalse(response.getNumberListField().contains(oldValue));
                finishTestAndReset();
              }
            });
          }
        });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistValueListReplace() {
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy fooProxy) {
            Request<SimpleFooProxy> updReq = req.simpleFooRequest().persistAndReturnSelf(
                fooProxy);
            fooProxy = updReq.edit(fooProxy);
            final ArrayList<Integer> al = new ArrayList<Integer>();
            al.add(5);
            al.add(8);
            al.add(13);
            fooProxy.setNumberListField(al);
            updReq.fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy response) {
                List<Integer> list = response.getNumberListField();
                assertEquals(5, (int) list.get(0));
                assertEquals(8, (int) list.get(1));
                assertEquals(13, (int) list.get(2));
                finishTestAndReset();
              }
            });
          }
        });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistValueListReverse() {
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy fooProxy) {
            Request<SimpleFooProxy> updReq = req.simpleFooRequest().persistAndReturnSelf(
                fooProxy);
            fooProxy = updReq.edit(fooProxy);
            final ArrayList<Integer> al = new ArrayList<Integer>();
            List<Integer> listField = fooProxy.getNumberListField();
            al.addAll(listField);
            Collections.reverse(listField);
            updReq.fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy response) {
                Collections.reverse(al);
                assertTrue(response.getNumberListField().equals(al));
                finishTestAndReset();
              }
            });
          }
        });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistValueListSetIndex() {
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy fooProxy) {
            Request<SimpleFooProxy> updReq = req.simpleFooRequest().persistAndReturnSelf(
                fooProxy);
            fooProxy = updReq.edit(fooProxy);
            fooProxy.getNumberListField().set(0, 10);
            updReq.fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy response) {
                assertTrue(response.getNumberListField().get(0) == 10);
                finishTestAndReset();
              }
            });
          }
        });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistValueSetAddNew() {
    delayTestFinish(5000);
    SimpleBarProxy newBar = req.create(SimpleBarProxy.class);

    req.simpleBarRequest().persistAndReturnSelf(newBar).fire(
        new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(final SimpleBarProxy barProxy) {
            req.simpleFooRequest().findSimpleFooById(999L).with(
                "oneToManySetField").fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy fooProxy) {
                Request<SimpleFooProxy> updReq = req.simpleFooRequest().persistAndReturnSelf(
                    fooProxy).with("oneToManySetField");
                fooProxy = updReq.edit(fooProxy);

                Set<SimpleBarProxy> setField = fooProxy.getOneToManySetField();
                final int listCount = setField.size();
                setField.add(barProxy);
                updReq.fire(new Receiver<SimpleFooProxy>() {
                  @Override
                  public void onSuccess(SimpleFooProxy response) {
                    assertEquals(listCount + 1,
                        response.getOneToManySetField().size());
                    assertContains(response.getOneToManySetField(), barProxy);
                    finishTestAndReset();
                  }
                });
              }
            });
          }
        });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistValueSetAlreadyExists() {
    delayTestFinish(5000);

    req.simpleBarRequest().findSimpleBarById("1L").fire(
        new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(final SimpleBarProxy barProxy) {
            req.simpleFooRequest().findSimpleFooById(999L).with(
                "oneToManySetField").fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy fooProxy) {
                Request<SimpleFooProxy> updReq = req.simpleFooRequest().persistAndReturnSelf(
                    fooProxy).with("oneToManySetField");
                fooProxy = updReq.edit(fooProxy);

                Set<SimpleBarProxy> setField = fooProxy.getOneToManySetField();
                final int listCount = setField.size();
                assertContains(setField, barProxy);
                setField.add(barProxy);
                updReq.fire(new Receiver<SimpleFooProxy>() {
                  @Override
                  public void onSuccess(SimpleFooProxy response) {
                    assertEquals(response.getOneToManySetField().size(),
                        listCount);
                    assertContains(response.getOneToManySetField(), barProxy);
                    finishTestAndReset();
                  }
                });
              }
            });
          }
        });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistValueSetRemove() {
    delayTestFinish(5000);

    req.simpleBarRequest().findSimpleBarById("1L").fire(
        new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(final SimpleBarProxy barProxy) {
            req.simpleFooRequest().findSimpleFooById(999L).with(
                "oneToManySetField").fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy fooProxy) {
                Request<SimpleFooProxy> updReq = req.simpleFooRequest().persistAndReturnSelf(
                    fooProxy).with("oneToManySetField");
                fooProxy = updReq.edit(fooProxy);

                Set<SimpleBarProxy> setField = fooProxy.getOneToManySetField();
                final int listCount = setField.size();
                assertContains(setField, barProxy);
                setField.remove(barProxy);
                assertNotContains(setField, barProxy);
                updReq.fire(new Receiver<SimpleFooProxy>() {
                  @Override
                  public void onSuccess(SimpleFooProxy response) {
                    assertEquals(listCount - 1,
                        response.getOneToManySetField().size());
                    assertNotContains(response.getOneToManySetField(), barProxy);
                    finishTestAndReset();
                  }
                });
              }
            });
          }
        });
  }

  public void testPrimitiveList() {
    delayTestFinish(5000);
    final Request<List<Integer>> fooReq = req.simpleFooRequest().getNumberList();
    fooReq.fire(new Receiver<List<Integer>>() {
      @Override
      public void onSuccess(List<Integer> response) {
        assertEquals(3, response.size());
        assertEquals(1, (int) response.get(0));
        assertEquals(2, (int) response.get(1));
        assertEquals(3, (int) response.get(2));
        finishTestAndReset();
      }
    });
  }

  public void testPrimitiveListAsParameter() {
    delayTestFinish(5000);
    final Request<SimpleFooProxy> fooReq = req.simpleFooRequest().findSimpleFooById(
        999L);
    fooReq.fire(new Receiver<SimpleFooProxy>() {
      public void onSuccess(SimpleFooProxy response) {
        final Request<Integer> sumReq = req.simpleFooRequest().sum(response,
            Arrays.asList(1, 2, 3));
        sumReq.fire(new Receiver<Integer>() {
          @Override
          public void onSuccess(Integer response) {
            assertEquals(6, response.intValue());
            finishTestAndReset();
          }
        });
      }
    });
  }

  public void testPrimitiveListBooleanAsParameter() {
    delayTestFinish(5000);

    Request<Boolean> procReq = req.simpleFooRequest().processBooleanList(
        Arrays.asList(true, false));

    procReq.fire(new Receiver<Boolean>() {
      @Override
      public void onSuccess(Boolean response) {
        assertEquals(true, (boolean) response);
        finishTestAndReset();
      }
    });
  }

  public void testPrimitiveListDateAsParameter() {
    delayTestFinish(5000);

    final Date date = new Date(90, 0, 1);
    Request<Date> procReq = req.simpleFooRequest().processDateList(
        Arrays.asList(date));
    procReq.fire(new Receiver<Date>() {
      @Override
      public void onSuccess(Date response) {
        assertEquals(date, response);
        finishTestAndReset();
      }
    });
  }

  public void testPrimitiveListEnumAsParameter() {
    delayTestFinish(5000);

    Request<SimpleEnum> procReq = req.simpleFooRequest().processEnumList(
        Arrays.asList(SimpleEnum.BAR));

    procReq.fire(new Receiver<SimpleEnum>() {
      @Override
      public void onSuccess(SimpleEnum response) {
        assertEquals(SimpleEnum.BAR, response);
        finishTestAndReset();
      }
    });
  }

  public void testPrimitiveSet() {
    delayTestFinish(5000);
    final Request<Set<Integer>> fooReq = req.simpleFooRequest().getNumberSet();
    fooReq.fire(new Receiver<Set<Integer>>() {
      @Override
      public void onSuccess(Set<Integer> response) {
        assertEquals(3, response.size());
        assertTrue(response.contains(1));
        assertTrue(response.contains(2));
        assertTrue(response.contains(3));
        finishTestAndReset();
      }
    });
  }

  public void testPrimitiveString() {
    delayTestFinish(5000);
    final String testString = "test\"string\'with\nstring\u2060characters\t";
    final Request<String> fooReq = req.simpleFooRequest().processString(
        testString);
    fooReq.fire(new Receiver<String>() {
      @Override
      public void onSuccess(String response) {
        assertEquals(testString, response);
        finishTestAndReset();
      }
    });
  }

  public void testProxyList() {
    delayTestFinish(5000);
    final Request<SimpleFooProxy> fooReq = req.simpleFooRequest().findSimpleFooById(
        999L).with("oneToManyField");
    fooReq.fire(new Receiver<SimpleFooProxy>() {
      @Override
      public void onSuccess(SimpleFooProxy response) {
        assertEquals(2, response.getOneToManyField().size());
        finishTestAndReset();
      }
    });
  }

  public void testProxyListAsParameter() {
    delayTestFinish(5000);
    final Request<SimpleFooProxy> fooReq = req.simpleFooRequest().findSimpleFooById(
        999L).with("selfOneToManyField");
    fooReq.fire(new Receiver<SimpleFooProxy>() {
      public void onSuccess(final SimpleFooProxy fooProxy) {
        final Request<String> procReq = req.simpleFooRequest().processList(
            fooProxy, fooProxy.getSelfOneToManyField());
        procReq.fire(new Receiver<String>() {
          @Override
          public void onSuccess(String response) {
            assertEquals(fooProxy.getUserName(), response);
            finishTestAndReset();
          }
        });
      }
    });
  }

  public void testProxysAsInstanceMethodParams() {
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response) {
            SimpleBarProxy bar = req.create(SimpleBarProxy.class);
            Request<String> helloReq = req.simpleFooRequest().hello(response,
                bar);
            bar = helloReq.edit(bar);
            bar.setUserName("BAR");
            helloReq.fire(new Receiver<String>() {
              @Override
              public void onSuccess(String response) {
                assertEquals("Greetings BAR from GWT", response);
                finishTestAndReset();
              }
            });
          }
        });
  }

  public void testServerFailureCheckedException() {
    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);
    final Request<SimpleFooProxy> persistRequest = req.simpleFooRequest().persistAndReturnSelf(
        newFoo);
    final SimpleFooProxy mutableFoo = persistRequest.edit(newFoo);
    // 43 is the crash causing magic number for a checked exception
    mutableFoo.setPleaseCrash(43);
    persistRequest.fire(new FooReciever(mutableFoo, persistRequest, ""));
  }

  public void testServerFailureRuntimeException() {
    delayTestFinish(5000);
    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);
    final Request<SimpleFooProxy> persistRequest = req.simpleFooRequest().persistAndReturnSelf(
        newFoo);
    final SimpleFooProxy mutableFoo = persistRequest.edit(newFoo);
    // 42 is the crash causing magic number for a runtime exception
    mutableFoo.setPleaseCrash(42);
    persistRequest.fire(new FooReciever(mutableFoo, persistRequest, ""));
  }

  /**
   * Tests the behaviors of setters and their effects on getters.
   */
  public void testSetters() {
    SimpleFooProxy foo = req.create(SimpleFooProxy.class);
    SimpleBarProxy bar = req.create(SimpleBarProxy.class);

    // Assert that uninitalize references are null
    assertNull(foo.getBarField());

    // Assert that objects must be made mutable before calling setters.
    try {
      foo.setBarField(bar);
      fail("Must require request to call setters");
    } catch (UnsupportedOperationException expected) {
    }

    Request<SimpleFooProxy> r = req.simpleFooRequest().persistAndReturnSelf(foo);
    foo = r.edit(foo);
    foo.setBarField(bar);

    // Assert that the set value is retained
    SimpleBarProxy returnedBarField = foo.getBarField();
    assertNotNull(returnedBarField);
    assertEquals(bar.stableId(), returnedBarField.stableId());
    assertEquals(returnedBarField, foo.getBarField());
    assertNotSame(returnedBarField, foo.getBarField());

    // Returned not equal to original because it's associated with a request
    assertFalse(bar.equals(returnedBarField));

    // Getters called on mutable objects are also mutable
    returnedBarField.setUserName("userName");
    assertEquals("userName", returnedBarField.getUserName());
  }

  /**
   * There's plenty of special-case code for Collection properties, so they need
   * to be tested as well.
   */
  public void testSettersWithCollections() {
    SimpleFooProxy foo = req.create(SimpleFooProxy.class);
    SimpleBarProxy bar = req.create(SimpleBarProxy.class);
    List<SimpleBarProxy> originalList = Collections.singletonList(bar);

    // Assert that uninitalize references are null
    assertNull(foo.getOneToManyField());

    // Assert that objects must be made mutable before calling setters.
    try {
      foo.setOneToManyField(null);
      fail("Must require request to call setters");
    } catch (UnsupportedOperationException expected) {
    }

    Request<SimpleFooProxy> r = req.simpleFooRequest().persistAndReturnSelf(foo);
    foo = r.edit(foo);
    foo.setOneToManyField(originalList);
    // There's a "dummy" create case here; AbstractRequest, DVS is untestable

    // Assert that the value is retained, but a defensive copy
    List<SimpleBarProxy> list = foo.getOneToManyField();
    assertNotSame(originalList, list);
    // Not equal because list is associated with a request now
    assertFalse(originalList.equals(list));
    assertEquals(1, list.size());
    assertEquals(bar.stableId(), list.get(0).stableId());
    assertEquals(list, foo.getOneToManyField());

    // Assert that entities returned from editable list are mutable
    list.get(0).setUserName("userName");
  }

  public void testSettersWithMutableObject() {
    SimpleFooProxy foo = req.create(SimpleFooProxy.class);
    Request<SimpleFooProxy> r = req.simpleFooRequest().persistAndReturnSelf(foo);
    foo = r.edit(foo);

    SimpleBarProxy immutableBar = req.create(SimpleBarProxy.class);
    SimpleBarProxy mutableBar = r.edit(immutableBar);
    mutableBar.setUserName("userName");
    foo.setBarField(mutableBar);

    // Creating a new editable object in the same request should read through
    r.edit(immutableBar).setUserName("Reset");
    assertEquals("Reset", foo.getBarField().getUserName());
  }

  public void testStableId() {
    delayTestFinish(5000);

    final SimpleFooProxy foo = req.create(SimpleFooProxy.class);
    final Object futureId = foo.getId();
    assertTrue(((ProxyImpl) foo).unpersisted());
    Request<SimpleFooProxy> fooReq = req.simpleFooRequest().persistAndReturnSelf(
        foo);

    final SimpleFooProxy newFoo = fooReq.edit(foo);
    assertEquals(futureId, foo.getId());
    assertTrue(((ProxyImpl) foo).unpersisted());
    assertEquals(futureId, newFoo.getId());
    assertTrue(((ProxyImpl) newFoo).unpersisted());

    newFoo.setUserName("GWT basic user");
    fooReq.fire(new Receiver<SimpleFooProxy>() {

      @Override
      public void onSuccess(final SimpleFooProxy returned) {
        assertEquals(futureId, foo.getId());
        assertTrue(((ProxyImpl) foo).unpersisted());
        assertEquals(futureId, newFoo.getId());
        assertTrue(((ProxyImpl) newFoo).unpersisted());

        assertFalse(((ProxyImpl) returned).unpersisted());

        checkStableIdEquals(foo, returned);
        checkStableIdEquals(newFoo, returned);
        Request<SimpleFooProxy> editRequest = req.simpleFooRequest().persistAndReturnSelf(
            returned);
        final SimpleFooProxy editableFoo = editRequest.edit(returned);
        editableFoo.setUserName("GWT power user");
        editRequest.fire(new Receiver<SimpleFooProxy>() {

          @Override
          public void onSuccess(SimpleFooProxy returnedAfterEdit) {
            checkStableIdEquals(editableFoo, returnedAfterEdit);
            assertEquals(returnedAfterEdit.getId(), returned.getId());
            finishTestAndReset();
          }
        });
      }
    });
  }

  public void testViolationAbsent() {
    delayTestFinish(5000);

    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);
    final Request<Void> fooReq = req.simpleFooRequest().persist(newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("Amit"); // will not cause violation.

    fooReq.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void ignore) {
        finishTestAndReset();
      }
    });
  }

  public void testViolationsOnCreate() {
    delayTestFinish(5000);

    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);
    final Request<SimpleFooProxy> create = req.simpleFooRequest().persistAndReturnSelf(
        newFoo);
    new FailFixAndRefire<SimpleFooProxy>(newFoo, create).doTest();
  }

  public void testViolationsOnCreateVoidReturn() {
    delayTestFinish(5000);

    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);
    final Request<Void> create = req.simpleFooRequest().persist(newFoo);
    new FailFixAndRefire<Void>(newFoo, create).doVoidTest();
  }

  public void testViolationsOnEdit() {
    delayTestFinish(5000);

    fooCreationRequest().fire(new Receiver<SimpleFooProxy>() {
      @Override
      public void onSuccess(SimpleFooProxy returned) {
        Request<SimpleFooProxy> editRequest = req.simpleFooRequest().persistAndReturnSelf(
            returned);
        new FailFixAndRefire<SimpleFooProxy>(returned, editRequest).doTest();
      }
    });
  }

  public void testViolationsOnEditVoidReturn() {
    delayTestFinish(5000);

    fooCreationRequest().fire(new Receiver<SimpleFooProxy>() {
      @Override
      public void onSuccess(SimpleFooProxy returned) {
        Request<Void> editRequest = req.simpleFooRequest().persist(returned);
        new FailFixAndRefire<Void>(returned, editRequest).doVoidTest();
      }
    });
  }

  private void assertCannotFire(final Request<Long> mutateRequest) {
    try {
      mutateRequest.fire(new Receiver<Long>() {
        @Override
        public void onSuccess(Long response) {
          fail("Should not be called");
        }
      });
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      /* cannot reuse a successful request, mores the pity */
    }
  }

  private Request<SimpleFooProxy> fooCreationRequest() {
    SimpleFooProxy originalFoo = req.create(SimpleFooProxy.class);
    final Request<SimpleFooProxy> fooReq = req.simpleFooRequest().persistAndReturnSelf(
        originalFoo);
    originalFoo = fooReq.edit(originalFoo);
    originalFoo.setUserName("GWT User");
    return fooReq;
  }
}
