package com.acousticguard.hub.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for BoundingBox domain value object.
 * Tests parsing, validation, and coordinate extraction.
 */
@DisplayName("BoundingBox")
class BoundingBoxTest {

    @Nested
    @DisplayName("When parsing from string")
    class WhenParsingFromString {

        @Test
        @DisplayName("Should parse valid bounding box string")
        void shouldParseValidBoundingBoxString() {
            BoundingBox bbox = BoundingBox.fromString("-122.5,37.7,-122.3,37.9");
            
            assertThat(bbox.minLng()).isEqualTo(-122.5f);
            assertThat(bbox.minLat()).isEqualTo(37.7f);
            assertThat(bbox.maxLng()).isEqualTo(-122.3f);
            assertThat(bbox.maxLat()).isEqualTo(37.9f);
        }

        @Test
        @DisplayName("Should parse bounding box with whitespace")
        void shouldParseBoundingBoxWithWhitespace() {
            BoundingBox bbox = BoundingBox.fromString(" -122.5 , 37.7 , -122.3 , 37.9 ");
            
            assertThat(bbox.minLng()).isEqualTo(-122.5f);
            assertThat(bbox.minLat()).isEqualTo(37.7f);
            assertThat(bbox.maxLng()).isEqualTo(-122.3f);
            assertThat(bbox.maxLat()).isEqualTo(37.9f);
        }

        @Test
        @DisplayName("Should throw exception for null string")
        void shouldThrowExceptionForNullString() {
            assertThatThrownBy(() -> BoundingBox.fromString(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Bounding box cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw exception for blank string")
        void shouldThrowExceptionForBlankString() {
            assertThatThrownBy(() -> BoundingBox.fromString(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Bounding box cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw exception for wrong number of coordinates")
        void shouldThrowExceptionForWrongNumberOfCoordinates() {
            assertThatThrownBy(() -> BoundingBox.fromString("-122.5,37.7,-122.3"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Bounding box must have exactly 4 coordinates in format 'minLng,minLat,maxLng,maxLat'");
        }

        @Test
        @DisplayName("Should throw exception for too many coordinates")
        void shouldThrowExceptionForTooManyCoordinates() {
            assertThatThrownBy(() -> BoundingBox.fromString("-122.5,37.7,-122.3,37.9,0"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Bounding box must have exactly 4 coordinates in format 'minLng,minLat,maxLng,maxLat'");
        }

        @Test
        @DisplayName("Should throw exception for non-numeric coordinates")
        void shouldThrowExceptionForNonNumericCoordinates() {
            assertThatThrownBy(() -> BoundingBox.fromString("invalid,37.7,-122.3,37.9"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Bounding box coordinates must be valid numbers");
        }
    }

    @Nested
    @DisplayName("When validating coordinates")
    class WhenValidatingCoordinates {

        @Test
        @DisplayName("Should throw exception for minLng out of range")
        void shouldThrowExceptionForMinLngOutOfRange() {
            assertThatThrownBy(() -> BoundingBox.fromString("-181,37.7,-122.3,37.9"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("minLng must be between -180 and 180");
        }

        @Test
        @DisplayName("Should throw exception for minLat out of range")
        void shouldThrowExceptionForMinLatOutOfRange() {
            assertThatThrownBy(() -> BoundingBox.fromString("-122.5,-91,-122.3,37.9"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("minLat must be between -90 and 90");
        }

        @Test
        @DisplayName("Should throw exception for maxLng out of range")
        void shouldThrowExceptionForMaxLngOutOfRange() {
            assertThatThrownBy(() -> BoundingBox.fromString("-122.5,37.7,181,37.9"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("maxLng must be between -180 and 180");
        }

        @Test
        @DisplayName("Should throw exception for maxLat out of range")
        void shouldThrowExceptionForMaxLatOutOfRange() {
            assertThatThrownBy(() -> BoundingBox.fromString("-122.5,37.7,-122.3,91"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("maxLat must be between -90 and 90");
        }

        @Test
        @DisplayName("Should throw exception when minLng > maxLng")
        void shouldThrowExceptionWhenMinLngGreaterThanMaxLng() {
            assertThatThrownBy(() -> BoundingBox.fromString("-122.3,37.7,-122.5,37.9"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("minLng must be less than or equal to maxLng");
        }

        @Test
        @DisplayName("Should throw exception when minLat > maxLat")
        void shouldThrowExceptionWhenMinLatGreaterThanMaxLat() {
            assertThatThrownBy(() -> BoundingBox.fromString("-122.5,37.9,-122.3,37.7"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("minLat must be less than or equal to maxLat");
        }

        @Test
        @DisplayName("Should accept equal min and max coordinates")
        void shouldAcceptEqualMinAndMaxCoordinates() {
            BoundingBox bbox = BoundingBox.fromString("-122.5,37.7,-122.5,37.7");
            
            assertThat(bbox.minLng()).isEqualTo(-122.5f);
            assertThat(bbox.minLat()).isEqualTo(37.7f);
            assertThat(bbox.maxLng()).isEqualTo(-122.5f);
            assertThat(bbox.maxLat()).isEqualTo(37.7f);
        }

        @Test
        @DisplayName("Should accept boundary values")
        void shouldAcceptBoundaryValues() {
            BoundingBox bbox = BoundingBox.fromString("-180,-90,180,90");
            
            assertThat(bbox.minLng()).isEqualTo(-180f);
            assertThat(bbox.minLat()).isEqualTo(-90f);
            assertThat(bbox.maxLng()).isEqualTo(180f);
            assertThat(bbox.maxLat()).isEqualTo(90f);
        }
    }

    @Nested
    @DisplayName("When extracting coordinates")
    class WhenExtractingCoordinates {

        @Test
        @DisplayName("Should return correct minLng")
        void shouldReturnCorrectMinLng() {
            BoundingBox bbox = BoundingBox.fromString("-122.5,37.7,-122.3,37.9");
            assertThat(bbox.minLng()).isEqualTo(-122.5f);
        }

        @Test
        @DisplayName("Should return correct minLat")
        void shouldReturnCorrectMinLat() {
            BoundingBox bbox = BoundingBox.fromString("-122.5,37.7,-122.3,37.9");
            assertThat(bbox.minLat()).isEqualTo(37.7f);
        }

        @Test
        @DisplayName("Should return correct maxLng")
        void shouldReturnCorrectMaxLng() {
            BoundingBox bbox = BoundingBox.fromString("-122.5,37.7,-122.3,37.9");
            assertThat(bbox.maxLng()).isEqualTo(-122.3f);
        }

        @Test
        @DisplayName("Should return correct maxLat")
        void shouldReturnCorrectMaxLat() {
            BoundingBox bbox = BoundingBox.fromString("-122.5,37.7,-122.3,37.9");
            assertThat(bbox.maxLat()).isEqualTo(37.9f);
        }
    }
}
