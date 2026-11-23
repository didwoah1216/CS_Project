package com.chagok.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScorePolicyTest {

    @Test
    void octoberAndSeptemberMatchTheReport() {
        assertEquals(6, ScorePolicy.flexPoints(91.0));
        assertEquals(17, ScorePolicy.budgetPoints(4, 6));
        assertEquals(3.4, ScorePolicy.changeRate(true, 751_000, 776_300));
        assertEquals(10, ScorePolicy.changePoints(3.4));
        assertEquals(0, ScorePolicy.peerPoints(91.0, 55.6));
        assertEquals(33, 6 + 17 + 10 + 0);
        assertEquals("D", ScorePolicy.grade(33));

        assertEquals(17, ScorePolicy.flexPoints(74.9));
        assertEquals(12, ScorePolicy.budgetPoints(0, 0));
        assertNull(ScorePolicy.changeRate(false, 0, 751_000));
        assertEquals(10, ScorePolicy.changePoints(null));
        assertEquals(5, ScorePolicy.peerPoints(74.9, 54.4));
        assertEquals(44, 17 + 12 + 10 + 5);
        assertEquals("D", ScorePolicy.grade(44));
    }

    @Test
    void boundariesForChangePeerAndGrade() {
        assertEquals(40, ScorePolicy.flexPoints(40));
        assertEquals(0, ScorePolicy.flexPoints(100));
        assertEquals(20, ScorePolicy.changePoints(-10.0));
        assertEquals(15, ScorePolicy.changePoints(-9.9));
        assertEquals(15, ScorePolicy.changePoints(0.0));
        assertEquals(10, ScorePolicy.changePoints(0.1));
        assertEquals(10, ScorePolicy.changePoints(10.0));
        assertEquals(5, ScorePolicy.changePoints(10.1));
        assertEquals(5, ScorePolicy.changePoints(30.0));
        assertEquals(0, ScorePolicy.changePoints(30.1));
        assertEquals(15, ScorePolicy.peerPoints(50, 50.0));
        assertEquals(10, ScorePolicy.peerPoints(60, 50.0));
        assertEquals(5, ScorePolicy.peerPoints(60.1, 50.0));
        assertEquals(5, ScorePolicy.peerPoints(80, 50.0));
        assertEquals(0, ScorePolicy.peerPoints(80.1, 50.0));
        assertEquals(8, ScorePolicy.peerPoints(50, null));
        assertEquals("A", ScorePolicy.grade(85));
        assertEquals("B", ScorePolicy.grade(70));
        assertEquals("C", ScorePolicy.grade(55));
        assertEquals("D", ScorePolicy.grade(54));
    }
}
