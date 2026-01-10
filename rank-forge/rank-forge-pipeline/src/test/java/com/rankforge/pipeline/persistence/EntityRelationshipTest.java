/*
 *
 *  *Copyright [2024] [Bageshwar Pratap Narain]
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.rankforge.pipeline.persistence;

import com.rankforge.pipeline.persistence.entity.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to validate JPA entity relationships are correctly configured.
 * 
 * These tests verify:
 * 1. @ManyToOne annotations are present with correct cascade settings
 * 2. @JoinColumn annotations are correctly configured
 * 3. Entity inheritance is set up properly
 * 4. All required entity types extend GameEventEntity
 * 
 * Author bageshwar.pn
 * Date 2026
 */
class EntityRelationshipTest {

    @Nested
    @DisplayName("GameEventEntity Base Class Tests")
    class GameEventEntityTests {

        @Test
        @DisplayName("GameEventEntity should have @ManyToOne game relationship with CascadeType.PERSIST")
        void shouldHaveManyToOneGameRelationship() throws NoSuchFieldException {
            // Given
            Field gameField = GameEventEntity.class.getDeclaredField("game");
            
            // Then
            assertTrue(gameField.isAnnotationPresent(ManyToOne.class), 
                "game field should have @ManyToOne annotation");
            
            ManyToOne manyToOne = gameField.getAnnotation(ManyToOne.class);
            assertEquals(FetchType.LAZY, manyToOne.fetch(), 
                "game relationship should be LAZY fetched");
            
            // Verify cascade includes PERSIST
            boolean hasPersistCascade = false;
            for (CascadeType cascade : manyToOne.cascade()) {
                if (cascade == CascadeType.PERSIST) {
                    hasPersistCascade = true;
                    break;
                }
            }
            assertTrue(hasPersistCascade, "game relationship should have CascadeType.PERSIST");
        }

        @Test
        @DisplayName("GameEventEntity should have @JoinColumn for gameId")
        void shouldHaveJoinColumnForGameId() throws NoSuchFieldException {
            // Given
            Field gameField = GameEventEntity.class.getDeclaredField("game");
            
            // Then
            assertTrue(gameField.isAnnotationPresent(JoinColumn.class),
                "game field should have @JoinColumn annotation");
            
            JoinColumn joinColumn = gameField.getAnnotation(JoinColumn.class);
            assertEquals("gameId", joinColumn.name(), 
                "@JoinColumn name should be 'gameId'");
        }

        @Test
        @DisplayName("GameEventEntity should have @ManyToOne roundStart relationship with CascadeType.PERSIST")
        void shouldHaveManyToOneRoundStartRelationship() throws NoSuchFieldException {
            // Given
            Field roundStartField = GameEventEntity.class.getDeclaredField("roundStart");
            
            // Then
            assertTrue(roundStartField.isAnnotationPresent(ManyToOne.class),
                "roundStart field should have @ManyToOne annotation");
            
            ManyToOne manyToOne = roundStartField.getAnnotation(ManyToOne.class);
            assertEquals(FetchType.LAZY, manyToOne.fetch(),
                "roundStart relationship should be LAZY fetched");
            
            // Verify cascade includes PERSIST
            boolean hasPersistCascade = false;
            for (CascadeType cascade : manyToOne.cascade()) {
                if (cascade == CascadeType.PERSIST) {
                    hasPersistCascade = true;
                    break;
                }
            }
            assertTrue(hasPersistCascade, "roundStart relationship should have CascadeType.PERSIST");
        }

        @Test
        @DisplayName("GameEventEntity should have @JoinColumn for roundStartEventId")
        void shouldHaveJoinColumnForRoundStartEventId() throws NoSuchFieldException {
            // Given
            Field roundStartField = GameEventEntity.class.getDeclaredField("roundStart");
            
            // Then
            assertTrue(roundStartField.isAnnotationPresent(JoinColumn.class),
                "roundStart field should have @JoinColumn annotation");
            
            JoinColumn joinColumn = roundStartField.getAnnotation(JoinColumn.class);
            assertEquals("roundStartEventId", joinColumn.name(),
                "@JoinColumn name should be 'roundStartEventId'");
        }

        @Test
        @DisplayName("GameEventEntity should use Single Table Inheritance")
        void shouldUseSingleTableInheritance() {
            // Given
            Class<GameEventEntity> entityClass = GameEventEntity.class;
            
            // Then
            assertTrue(entityClass.isAnnotationPresent(Inheritance.class),
                "GameEventEntity should have @Inheritance annotation");
            assertTrue(entityClass.isAnnotationPresent(DiscriminatorColumn.class),
                "GameEventEntity should have @DiscriminatorColumn annotation");
            
            Inheritance inheritance = entityClass.getAnnotation(Inheritance.class);
            assertEquals(InheritanceType.SINGLE_TABLE, inheritance.strategy(),
                "Should use SINGLE_TABLE inheritance strategy");
            
            DiscriminatorColumn discriminator = entityClass.getAnnotation(DiscriminatorColumn.class);
            assertEquals("gameEventType", discriminator.name(),
                "Discriminator column should be 'gameEventType'");
        }
    }

    @Nested
    @DisplayName("AccoladeEntity Tests")
    class AccoladeEntityTests {

        @Test
        @DisplayName("AccoladeEntity should have @ManyToOne game relationship with CascadeType.PERSIST")
        void shouldHaveManyToOneGameRelationship() throws NoSuchFieldException {
            // Given
            Field gameField = AccoladeEntity.class.getDeclaredField("game");
            
            // Then
            assertTrue(gameField.isAnnotationPresent(ManyToOne.class),
                "game field should have @ManyToOne annotation");
            
            ManyToOne manyToOne = gameField.getAnnotation(ManyToOne.class);
            assertEquals(FetchType.LAZY, manyToOne.fetch(),
                "game relationship should be LAZY fetched");
            
            // Verify cascade includes PERSIST
            boolean hasPersistCascade = false;
            for (CascadeType cascade : manyToOne.cascade()) {
                if (cascade == CascadeType.PERSIST) {
                    hasPersistCascade = true;
                    break;
                }
            }
            assertTrue(hasPersistCascade, "game relationship should have CascadeType.PERSIST");
        }

        @Test
        @DisplayName("AccoladeEntity should have non-nullable @JoinColumn for gameId")
        void shouldHaveNonNullableJoinColumnForGameId() throws NoSuchFieldException {
            // Given
            Field gameField = AccoladeEntity.class.getDeclaredField("game");
            
            // Then
            assertTrue(gameField.isAnnotationPresent(JoinColumn.class),
                "game field should have @JoinColumn annotation");
            
            JoinColumn joinColumn = gameField.getAnnotation(JoinColumn.class);
            assertEquals("gameId", joinColumn.name(),
                "@JoinColumn name should be 'gameId'");
            assertFalse(joinColumn.nullable(),
                "@JoinColumn should be non-nullable for accolades");
        }
    }

    @Nested
    @DisplayName("Event Entity Subclass Tests")
    class EventEntitySubclassTests {

        @Test
        @DisplayName("KillEventEntity should extend GameEventEntity")
        void killEventEntityShouldExtendGameEventEntity() {
            assertTrue(GameEventEntity.class.isAssignableFrom(KillEventEntity.class));
            assertTrue(KillEventEntity.class.isAnnotationPresent(DiscriminatorValue.class));
            assertEquals("KILL", KillEventEntity.class.getAnnotation(DiscriminatorValue.class).value());
        }

        @Test
        @DisplayName("AssistEventEntity should extend GameEventEntity")
        void assistEventEntityShouldExtendGameEventEntity() {
            assertTrue(GameEventEntity.class.isAssignableFrom(AssistEventEntity.class));
            assertTrue(AssistEventEntity.class.isAnnotationPresent(DiscriminatorValue.class));
            assertEquals("ASSIST", AssistEventEntity.class.getAnnotation(DiscriminatorValue.class).value());
        }

        @Test
        @DisplayName("AttackEventEntity should extend GameEventEntity")
        void attackEventEntityShouldExtendGameEventEntity() {
            assertTrue(GameEventEntity.class.isAssignableFrom(AttackEventEntity.class));
            assertTrue(AttackEventEntity.class.isAnnotationPresent(DiscriminatorValue.class));
            assertEquals("ATTACK", AttackEventEntity.class.getAnnotation(DiscriminatorValue.class).value());
        }

        @Test
        @DisplayName("BombEventEntity should extend GameEventEntity")
        void bombEventEntityShouldExtendGameEventEntity() {
            assertTrue(GameEventEntity.class.isAssignableFrom(BombEventEntity.class));
            assertTrue(BombEventEntity.class.isAnnotationPresent(DiscriminatorValue.class));
            assertEquals("BOMB_EVENT", BombEventEntity.class.getAnnotation(DiscriminatorValue.class).value());
        }

        @Test
        @DisplayName("RoundStartEventEntity should extend GameEventEntity")
        void roundStartEventEntityShouldExtendGameEventEntity() {
            assertTrue(GameEventEntity.class.isAssignableFrom(RoundStartEventEntity.class));
            assertTrue(RoundStartEventEntity.class.isAnnotationPresent(DiscriminatorValue.class));
            assertEquals("ROUND_START", RoundStartEventEntity.class.getAnnotation(DiscriminatorValue.class).value());
        }

        @Test
        @DisplayName("RoundEndEventEntity should extend GameEventEntity")
        void roundEndEventEntityShouldExtendGameEventEntity() {
            assertTrue(GameEventEntity.class.isAssignableFrom(RoundEndEventEntity.class));
            assertTrue(RoundEndEventEntity.class.isAnnotationPresent(DiscriminatorValue.class));
            assertEquals("ROUND_END", RoundEndEventEntity.class.getAnnotation(DiscriminatorValue.class).value());
        }

        @Test
        @DisplayName("GameOverEventEntity should extend GameEventEntity")
        void gameOverEventEntityShouldExtendGameEventEntity() {
            assertTrue(GameEventEntity.class.isAssignableFrom(GameOverEventEntity.class));
            assertTrue(GameOverEventEntity.class.isAnnotationPresent(DiscriminatorValue.class));
            assertEquals("GAME_OVER", GameOverEventEntity.class.getAnnotation(DiscriminatorValue.class).value());
        }

        @Test
        @DisplayName("GameProcessedEventEntity should extend GameEventEntity")
        void gameProcessedEventEntityShouldExtendGameEventEntity() {
            assertTrue(GameEventEntity.class.isAssignableFrom(GameProcessedEventEntity.class));
            assertTrue(GameProcessedEventEntity.class.isAnnotationPresent(DiscriminatorValue.class));
            assertEquals("GAME_PROCESSED", GameProcessedEventEntity.class.getAnnotation(DiscriminatorValue.class).value());
        }
    }

    @Nested
    @DisplayName("Entity Relationship Runtime Tests")
    class EntityRelationshipRuntimeTests {

        @Test
        @DisplayName("Should be able to set game reference on GameEventEntity")
        void shouldBeAbleToSetGameReferenceOnGameEventEntity() {
            // Given
            GameEntity game = new GameEntity();
            game.setMap("de_dust2");
            game.setGameOverTimestamp(Instant.now());
            game.setEndTime(Instant.now());
            game.setTeam1Score(16);
            game.setTeam2Score(14);
            
            KillEventEntity kill = new KillEventEntity(Instant.now());
            
            // When
            kill.setGame(game);
            
            // Then
            assertSame(game, kill.getGame());
        }

        @Test
        @DisplayName("Should be able to set roundStart reference on GameEventEntity")
        void shouldBeAbleToSetRoundStartReferenceOnGameEventEntity() {
            // Given
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            KillEventEntity kill = new KillEventEntity(Instant.now());
            
            // When
            kill.setRoundStart(roundStart);
            
            // Then
            assertSame(roundStart, kill.getRoundStart());
        }

        @Test
        @DisplayName("Should be able to set game reference on AccoladeEntity")
        void shouldBeAbleToSetGameReferenceOnAccoladeEntity() {
            // Given
            GameEntity game = new GameEntity();
            game.setMap("de_dust2");
            game.setGameOverTimestamp(Instant.now());
            game.setEndTime(Instant.now());
            game.setTeam1Score(16);
            game.setTeam2Score(14);
            
            AccoladeEntity accolade = new AccoladeEntity();
            accolade.setType("MVP");
            accolade.setPlayerName("Player1");
            accolade.setValue(1.0);
            accolade.setPosition(1);
            accolade.setScore(100.0);
            
            // When
            accolade.setGame(game);
            
            // Then
            assertSame(game, accolade.getGame());
        }

        @Test
        @DisplayName("Multiple events can share the same game reference")
        void multipleEventsCanShareSameGameReference() {
            // Given
            GameEntity game = new GameEntity();
            game.setMap("de_dust2");
            game.setGameOverTimestamp(Instant.now());
            game.setEndTime(Instant.now());
            game.setTeam1Score(16);
            game.setTeam2Score(14);
            
            KillEventEntity kill1 = new KillEventEntity(Instant.now());
            KillEventEntity kill2 = new KillEventEntity(Instant.now());
            AssistEventEntity assist = new AssistEventEntity(Instant.now());
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            
            // When
            kill1.setGame(game);
            kill2.setGame(game);
            assist.setGame(game);
            roundStart.setGame(game);
            
            // Then
            assertSame(game, kill1.getGame());
            assertSame(game, kill2.getGame());
            assertSame(game, assist.getGame());
            assertSame(game, roundStart.getGame());
        }

        @Test
        @DisplayName("Multiple events can share the same roundStart reference")
        void multipleEventsCanShareSameRoundStartReference() {
            // Given
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            
            KillEventEntity kill1 = new KillEventEntity(Instant.now());
            KillEventEntity kill2 = new KillEventEntity(Instant.now());
            AssistEventEntity assist = new AssistEventEntity(Instant.now());
            AttackEventEntity attack = new AttackEventEntity(Instant.now());
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now());
            
            // When
            kill1.setRoundStart(roundStart);
            kill2.setRoundStart(roundStart);
            assist.setRoundStart(roundStart);
            attack.setRoundStart(roundStart);
            roundEnd.setRoundStart(roundStart);
            
            // Then
            assertSame(roundStart, kill1.getRoundStart());
            assertSame(roundStart, kill2.getRoundStart());
            assertSame(roundStart, assist.getRoundStart());
            assertSame(roundStart, attack.getRoundStart());
            assertSame(roundStart, roundEnd.getRoundStart());
        }

        @Test
        @DisplayName("Multiple accolades can share the same game reference")
        void multipleAccoladesCanShareSameGameReference() {
            // Given
            GameEntity game = new GameEntity();
            game.setMap("de_dust2");
            game.setGameOverTimestamp(Instant.now());
            game.setEndTime(Instant.now());
            game.setTeam1Score(16);
            game.setTeam2Score(14);
            
            AccoladeEntity mvp = new AccoladeEntity();
            mvp.setType("MVP");
            mvp.setPlayerName("Player1");
            mvp.setValue(1.0);
            mvp.setPosition(1);
            mvp.setScore(100.0);
            
            AccoladeEntity topKills = new AccoladeEntity();
            topKills.setType("TopKills");
            topKills.setPlayerName("Player2");
            topKills.setValue(25.0);
            topKills.setPosition(2);
            topKills.setScore(85.0);
            
            // When
            mvp.setGame(game);
            topKills.setGame(game);
            
            // Then
            assertSame(game, mvp.getGame());
            assertSame(game, topKills.getGame());
        }
    }

    @Nested
    @DisplayName("GameEntity Tests")
    class GameEntityTests {

        @Test
        @DisplayName("GameEntity should have @Entity annotation")
        void shouldHaveEntityAnnotation() {
            assertTrue(GameEntity.class.isAnnotationPresent(Entity.class),
                "GameEntity should have @Entity annotation");
        }

        @Test
        @DisplayName("GameEntity should have @Table annotation with correct name")
        void shouldHaveTableAnnotation() {
            assertTrue(GameEntity.class.isAnnotationPresent(Table.class),
                "GameEntity should have @Table annotation");
            
            Table table = GameEntity.class.getAnnotation(Table.class);
            assertEquals("Game", table.name(),
                "@Table name should be 'Game'");
        }

        @Test
        @DisplayName("GameEntity should have @Id annotation on id field")
        void shouldHaveIdAnnotation() throws NoSuchFieldException {
            Field idField = GameEntity.class.getDeclaredField("id");
            
            assertTrue(idField.isAnnotationPresent(Id.class),
                "id field should have @Id annotation");
            assertTrue(idField.isAnnotationPresent(GeneratedValue.class),
                "id field should have @GeneratedValue annotation");
            
            GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
            assertEquals(GenerationType.IDENTITY, generatedValue.strategy(),
                "@GeneratedValue should use IDENTITY strategy");
        }

        @Test
        @DisplayName("GameEntity should have required timestamp fields")
        void shouldHaveRequiredTimestampFields() throws NoSuchFieldException {
            // gameOverTimestamp
            Field gameOverTimestampField = GameEntity.class.getDeclaredField("gameOverTimestamp");
            assertTrue(gameOverTimestampField.isAnnotationPresent(Column.class));
            Column gameOverCol = gameOverTimestampField.getAnnotation(Column.class);
            assertFalse(gameOverCol.nullable(), "gameOverTimestamp should be non-nullable");
            
            // endTime
            Field endTimeField = GameEntity.class.getDeclaredField("endTime");
            assertTrue(endTimeField.isAnnotationPresent(Column.class));
            Column endTimeCol = endTimeField.getAnnotation(Column.class);
            assertFalse(endTimeCol.nullable(), "endTime should be non-nullable");
        }
    }
}
