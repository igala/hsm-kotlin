package de.artcom.hsm.test;

import org.hamcrest.Matchers;
import org.hamcrest.collection.IsMapContaining;
import org.hamcrest.core.IsEqual;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import de.artcom.hsm.Action;
import de.artcom.hsm.Guard;
import de.artcom.hsm.State;
import de.artcom.hsm.StateMachine;
import de.artcom.hsm.Sub;
import de.artcom.hsm.TransitionKind;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class EventHandlingTest {

    @Test
    public void runToCompletionTest() {
        // given:
        State rawEgg = new State("rawEgg");
        State softEgg = new State("softEgg");
        State hardEgg = new State("hardEgg");
        final StateMachine sm = new StateMachine(rawEgg, softEgg, hardEgg);

        Action onEnterHardEgg = mock(Action.class);
        Action onEnterSoftEgg = mock(Action.class);

        softEgg.onEnter(onEnterSoftEgg);
        hardEgg.onEnter(onEnterHardEgg);

        Action boilAction = new Action() {
            @Override
            public void run() {
                sm.handleEvent("boil_too_long");
            }
        };
        rawEgg.addHandler("boil", softEgg, TransitionKind.External, boilAction);

        Action boilTooLongAction = mock(Action.class);
        rawEgg.addHandler("boil_too_long", hardEgg, TransitionKind.External, boilTooLongAction);

        Action boilTooLongAction2 = mock(Action.class);
        softEgg.addHandler("boil_too_long", softEgg, TransitionKind.Internal, boilTooLongAction2);

        sm.init();

        // when:
        sm.handleEvent("boil");

        // then:
        verifyZeroInteractions(onEnterHardEgg);
        verifyZeroInteractions(boilTooLongAction);
        verify(onEnterSoftEgg).run();
        verify(boilTooLongAction2).run();
    }

    @Test
    public void dontBubbleUpTest() {
        // given:
        Action enterA2 = mock(Action.class);
        Action enterB = mock(Action.class);

        State a1 = new State("a1");
        State a2 = new State("a2").onEnter(enterA2);
        Sub a = new Sub("a", a1, a2);
        State b = new State("b").onEnter(enterB);

        a1.addHandler("T1", a2, TransitionKind.External, new Guard() {
            @Override
            public boolean evaluate(@Nullable Map<String, ?> payload) {
                return payload.containsKey("foo");
            }
        });
        a.addHandler("T1", b, TransitionKind.External);

        StateMachine sm = new StateMachine(a, b);
        sm.init();

        // when:
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("foo", "bar");
        sm.handleEvent("T1", payload);

        // then:
        verify(enterA2).run();
        verifyZeroInteractions(enterB);
    }

    @Test
    public void bubbleUpTest() {
        // given:
        Action enterA2 = mock(Action.class);
        Action enterB = mock(Action.class);

        State a1 = new State("a1");
        State a2 = new State("a2").onEnter(enterA2);
        Sub a = new Sub("a", a1, a2);
        State b = new State("b").onEnter(enterB);

        a1.addHandler("T1", a2, TransitionKind.External, new Guard() {

            @Override
            public boolean evaluate(@Nullable Map<String, ?> payload) {
                return payload.containsKey("foo");
            }
        });
        a.addHandler("T1", b, TransitionKind.External);

        StateMachine sm = new StateMachine(a, b);
        sm.init();

        // when:
        sm.handleEvent("T1");

        // then:
        verify(enterB).run();
        verifyZeroInteractions(enterA2);
    }

    @Test
    public void handleTransitionWithPayload() {
        // given
        State a1 = new State("a1");
        State a2 = new State("a2")
        .onEnter(new Action() {
            @Override
            public void run() {
                assertThat(mPayload, Matchers.hasKey("foo"));
            }
        });

        a1.addHandler("T1", a2, TransitionKind.External)
                .onExit(new Action() {
                    @Override
                    public void run() {
                        assertThat(mPayload, Matchers.hasKey("foo"));
                    }
                });

        StateMachine sm = new StateMachine(a1, a2);
        sm.init();

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("foo", "bar");

        // when
        sm.handleEvent("T1", payload);
    }

    @Test
    public void initWithPayload() {
        // given
        Action a1Enter = new Action() {
            @Override
            public void run() {
                // then
                assertThat(mPayload, IsMapContaining.hasKey("foo"));
            }
        };
        State a1 = new State("a1").onEnter(a1Enter);
        StateMachine sm = new StateMachine(a1);
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("foo", "bar");

        // when
        sm.init(payload);
    }

    @Test
    public void initWithNullPayload() {
        // given
        Action a1Enter = mock(Action.class);
        State a1 = new State("a1").onEnter(a1Enter);
        StateMachine sm = new StateMachine(a1);

        // when
        try {
            sm.init(null);
        } catch (NullPointerException npe) {
            fail("StateMachine.init() should instantiate a new payload instead of throwing npe");
        }

        // then
    }

    @Test
    public void teardownWithPayload() {
        // when
        State a1 = new State("a1");
        State a = new Sub("a", a1);
        State b1 = new State("b1").onEnter(new Action() {
            @Override
            public void run() {
                assertThat(mPayload, IsMapContaining.hasKey("foo"));
            }
        });

        a1.addHandler("T1", b1, TransitionKind.External).onExit(new Action() {
            @Override
            public void run() {
                mPayload.put("foo",null);
            }
        });

        State b = new Sub("b", a, b1);
        StateMachine sm = new StateMachine(b);
        sm.init();
        sm.handleEvent("T1");
    }
}






















