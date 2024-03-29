/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class MediaTypeTests {

	@Test
	void testToString() {
		MediaType mediaType = new MediaType("text", "plain", 0.7);
		String result = mediaType.toString();
		assertThat(result).as("Invalid toString() returned").isEqualTo("text/plain;q=0.7");
	}

	@Test
	void slashInType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MediaType("text/plain"));
	}

	@Test
	void slashInSubtype() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MediaType("text", "/"));
	}

	@Test
	void getDefaultQualityValue() {
		MediaType mediaType = new MediaType("text", "plain");
		assertThat(mediaType.getQualityValue()).as("Invalid quality value").isCloseTo(1D, within(0D));
	}

	@Test
	void parseMediaType() {
		String s = "audio/*; q=0.2";
		MediaType mediaType = MediaType.parseMediaType(s);
		assertThat(mediaType.getType()).as("Invalid type").isEqualTo("audio");
		assertThat(mediaType.getSubtype()).as("Invalid subtype").isEqualTo("*");
		assertThat(mediaType.getQualityValue()).as("Invalid quality factor").isCloseTo(0.2D, within(0D));
	}

	@Test
	void parseMediaTypeNoSubtype() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio"));
	}

	@Test
	void parseMediaTypeNoSubtypeSlash() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/"));
	}

	@Test
	void parseMediaTypeTypeRange() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("*/json"));
	}

	@Test
	void parseMediaTypeIllegalType() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio(/basic"));
	}

	@Test
	void parseMediaTypeIllegalSubtype() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/basic)"));
	}

	@Test
	void parseMediaTypeEmptyParameterAttribute() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/*;=value"));
	}

	@Test
	void parseMediaTypeEmptyParameterValue() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/*;attr="));
	}

	@Test
	void parseMediaTypeIllegalParameterAttribute() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/*;attr<=value"));
	}

	@Test
	void parseMediaTypeIllegalParameterValue() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/*;attr=v>alue"));
	}

	@Test
	void parseMediaTypeIllegalQualityFactor() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("audio/basic;q=1.1"));
	}

	@Test
	void parseMediaTypeIllegalCharset() {
		assertThatExceptionOfType(InvalidMediaTypeException.class).isThrownBy(() ->
				MediaType.parseMediaType("text/html; charset=foo-bar"));
	}

	@Test
	void parseURLConnectionMediaType() {
		String s = "*; q=.2";
		MediaType mediaType = MediaType.parseMediaType(s);
		assertThat(mediaType.getType()).as("Invalid type").isEqualTo("*");
		assertThat(mediaType.getSubtype()).as("Invalid subtype").isEqualTo("*");
		assertThat(mediaType.getQualityValue()).as("Invalid quality factor").isCloseTo(0.2D, within(0D));
	}

	@Test
	void parseMediaTypes() {
		String s = "text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c";
		List<MediaType> mediaTypes = MediaType.parseMediaTypes(s);
		assertThat(mediaTypes).as("No media types returned").isNotNull();
		assertThat(mediaTypes).as("Invalid amount of media types").hasSize(4);

		mediaTypes = MediaType.parseMediaTypes("");
		assertThat(mediaTypes).as("No media types returned").isNotNull();
		assertThat(mediaTypes).as("Invalid amount of media types").isEmpty();
	}

	@Test  // gh-23241
	public void parseMediaTypesWithTrailingComma() {
		List<MediaType> mediaTypes = MediaType.parseMediaTypes("text/plain, text/html, ");
		assertThat(mediaTypes).as("No media types returned").isNotNull();
		assertThat(mediaTypes).as("Incorrect number of media types").hasSize(2);
	}

	@Test
	void compareTo() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audio = new MediaType("audio");
		MediaType audioWave = new MediaType("audio", "wave");
		MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
		MediaType audioBasic07 = new MediaType("audio", "basic", 0.7);

		// equal
		assertThat(audioBasic.compareTo(audioBasic)).as("Invalid comparison result").isEqualTo(0);
		assertThat(audio.compareTo(audio)).as("Invalid comparison result").isEqualTo(0);
		assertThat(audioBasicLevel.compareTo(audioBasicLevel)).as("Invalid comparison result").isEqualTo(0);

		assertThat(audioBasicLevel.compareTo(audio)).as("Invalid comparison result").isGreaterThan(0);

		List<MediaType> expected = new ArrayList<>();
		expected.add(audio);
		expected.add(audioBasic);
		expected.add(audioBasicLevel);
		expected.add(audioBasic07);
		expected.add(audioWave);

		List<MediaType> result = new ArrayList<>(expected);
		Random rnd = new Random();
		// shuffle & sort 10 times
		for (int i = 0; i < 10; i++) {
			Collections.shuffle(result, rnd);
			Collections.sort(result);

			for (int j = 0; j < result.size(); j++) {
				assertThat(result.get(j)).as("Invalid media type at " + j + ", run " + i).isSameAs(expected.get(j));
			}
		}
	}

	@Test
	void compareToConsistentWithEquals() {
		MediaType m1 = MediaType.parseMediaType("text/html; q=0.7; charset=iso-8859-1");
		MediaType m2 = MediaType.parseMediaType("text/html; charset=iso-8859-1; q=0.7");

		assertThat(m2).as("Media types not equal").isEqualTo(m1);
		assertThat(m1.compareTo(m2)).as("compareTo() not consistent with equals").isEqualTo(0);
		assertThat(m2.compareTo(m1)).as("compareTo() not consistent with equals").isEqualTo(0);

		m1 = MediaType.parseMediaType("text/html; q=0.7; charset=iso-8859-1");
		m2 = MediaType.parseMediaType("text/html; Q=0.7; charset=iso-8859-1");
		assertThat(m2).as("Media types not equal").isEqualTo(m1);
		assertThat(m1.compareTo(m2)).as("compareTo() not consistent with equals").isEqualTo(0);
		assertThat(m2.compareTo(m1)).as("compareTo() not consistent with equals").isEqualTo(0);
	}

	@Test
	void compareToCaseSensitivity() {
		MediaType m1 = new MediaType("audio", "basic");
		MediaType m2 = new MediaType("Audio", "Basic");
		assertThat(m1.compareTo(m2)).as("Invalid comparison result").isEqualTo(0);
		assertThat(m2.compareTo(m1)).as("Invalid comparison result").isEqualTo(0);

		m1 = new MediaType("audio", "basic", Collections.singletonMap("foo", "bar"));
		m2 = new MediaType("audio", "basic", Collections.singletonMap("Foo", "bar"));
		assertThat(m1.compareTo(m2)).as("Invalid comparison result").isEqualTo(0);
		assertThat(m2.compareTo(m1)).as("Invalid comparison result").isEqualTo(0);

		m1 = new MediaType("audio", "basic", Collections.singletonMap("foo", "bar"));
		m2 = new MediaType("audio", "basic", Collections.singletonMap("foo", "Bar"));
		assertThat(m1.compareTo(m2)).as("Invalid comparison result").isNotEqualTo(0);
		assertThat(m2.compareTo(m1)).as("Invalid comparison result").isNotEqualTo(0);


	}

	@Test
	void isMoreSpecific() {
		MediaType audio = new MediaType("audio");
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audioBasic07 = new MediaType("audio", "basic", 0.7);
		MediaType audioBasic03 = new MediaType("audio", "basic", 0.3);

		assertThat(audioBasic.isMoreSpecific(audio)).isTrue();
		assertThat(audio.isMoreSpecific(audioBasic)).isFalse();

		assertThat(audio.isMoreSpecific(audioBasic07)).isTrue();
		assertThat(audioBasic07.isMoreSpecific(audio)).isFalse();

		assertThat(audioBasic07.isMoreSpecific(audioBasic03)).isTrue();
		assertThat(audioBasic03.isMoreSpecific(audioBasic07)).isFalse();

		assertThat(audioBasic.isMoreSpecific(MediaType.TEXT_HTML)).isFalse();
	}

	@Test
	void isLessSpecific() {
		MediaType audio = new MediaType("audio");
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audioBasic07 = new MediaType("audio", "basic", 0.7);
		MediaType audioBasic03 = new MediaType("audio", "basic", 0.3);

		assertThat(audioBasic.isLessSpecific(audio)).isFalse();
		assertThat(audio.isLessSpecific(audioBasic)).isTrue();

		assertThat(audio.isLessSpecific(audioBasic07)).isFalse();
		assertThat(audioBasic07.isLessSpecific(audio)).isTrue();

		assertThat(audioBasic07.isLessSpecific(audioBasic03)).isFalse();
		assertThat(audioBasic03.isLessSpecific(audioBasic07)).isTrue();

		assertThat(audioBasic.isLessSpecific(MediaType.TEXT_HTML)).isFalse();
	}

	@Test
	void specificityComparator() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audioWave = new MediaType("audio", "wave");
		MediaType audio = new MediaType("audio");
		MediaType audio03 = new MediaType("audio", "*", 0.3);
		MediaType audio07 = new MediaType("audio", "*", 0.7);
		MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
		MediaType textHtml = new MediaType("text", "html");
		MediaType allXml = new MediaType("application", "*+xml");
		MediaType all = MediaType.ALL;

		@SuppressWarnings("removal")
		Comparator<MediaType> comp = MediaType.SPECIFICITY_COMPARATOR;

		// equal
		assertThat(comp.compare(audioBasic, audioBasic)).as("Invalid comparison result").isEqualTo(0);
		assertThat(comp.compare(audio, audio)).as("Invalid comparison result").isEqualTo(0);
		assertThat(comp.compare(audio07, audio07)).as("Invalid comparison result").isEqualTo(0);
		assertThat(comp.compare(audio03, audio03)).as("Invalid comparison result").isEqualTo(0);
		assertThat(comp.compare(audioBasicLevel, audioBasicLevel)).as("Invalid comparison result").isEqualTo(0);

		// specific to unspecific
		assertThat(comp.compare(audioBasic, audio)).as("Invalid comparison result").isLessThan(0);
		assertThat(comp.compare(audioBasic, all)).as("Invalid comparison result").isLessThan(0);
		assertThat(comp.compare(audio, all)).as("Invalid comparison result").isLessThan(0);
		assertThat(comp.compare(MediaType.APPLICATION_XHTML_XML, allXml)).as("Invalid comparison result").isLessThan(0);

		// unspecific to specific
		assertThat(comp.compare(audio, audioBasic)).as("Invalid comparison result").isGreaterThan(0);
		assertThat(comp.compare(allXml, MediaType.APPLICATION_XHTML_XML)).as("Invalid comparison result")
				.isGreaterThan(0);
		assertThat(comp.compare(all, audioBasic)).as("Invalid comparison result").isGreaterThan(0);
		assertThat(comp.compare(all, audio)).as("Invalid comparison result").isGreaterThan(0);

		// qualifiers
		assertThat(comp.compare(audio, audio07)).as("Invalid comparison result").isLessThan(0);
		assertThat(comp.compare(audio07, audio)).as("Invalid comparison result").isGreaterThan(0);
		assertThat(comp.compare(audio07, audio03)).as("Invalid comparison result").isLessThan(0);
		assertThat(comp.compare(audio03, audio07)).as("Invalid comparison result").isGreaterThan(0);
		assertThat(comp.compare(audio03, all)).as("Invalid comparison result").isLessThan(0);
		assertThat(comp.compare(all, audio03)).as("Invalid comparison result").isGreaterThan(0);

		// other parameters
		assertThat(comp.compare(audioBasic, audioBasicLevel)).as("Invalid comparison result").isGreaterThan(0);
		assertThat(comp.compare(audioBasicLevel, audioBasic)).as("Invalid comparison result").isLessThan(0);

		// different types
		assertThat(comp.compare(audioBasic, textHtml)).as("Invalid comparison result").isEqualTo(0);
		assertThat(comp.compare(textHtml, audioBasic)).as("Invalid comparison result").isEqualTo(0);

		// different subtypes
		assertThat(comp.compare(audioBasic, audioWave)).as("Invalid comparison result").isEqualTo(0);
		assertThat(comp.compare(audioWave, audioBasic)).as("Invalid comparison result").isEqualTo(0);
	}

	@Test
	@SuppressWarnings("removal")
	public void sortBySpecificityRelated() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audio = new MediaType("audio");
		MediaType audio03 = new MediaType("audio", "*", 0.3);
		MediaType audio07 = new MediaType("audio", "*", 0.7);
		MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
		MediaType all = MediaType.ALL;

		List<MediaType> expected = new ArrayList<>();
		expected.add(audioBasicLevel);
		expected.add(audioBasic);
		expected.add(audio);
		expected.add(audio07);
		expected.add(audio03);
		expected.add(all);

		List<MediaType> result = new ArrayList<>(expected);
		Random rnd = new Random();
		// shuffle & sort 10 times
		for (int i = 0; i < 10; i++) {
			Collections.shuffle(result, rnd);
			MediaType.sortBySpecificity(result);

			for (int j = 0; j < result.size(); j++) {
				assertThat(result.get(j)).as("Invalid media type at " + j).isSameAs(expected.get(j));
			}
		}
	}

	@Test
	@SuppressWarnings("removal")
	public void sortBySpecificityUnrelated() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audioWave = new MediaType("audio", "wave");
		MediaType textHtml = new MediaType("text", "html");

		List<MediaType> expected = new ArrayList<>();
		expected.add(textHtml);
		expected.add(audioBasic);
		expected.add(audioWave);

		List<MediaType> result = new ArrayList<>(expected);
		MediaType.sortBySpecificity(result);

		for (int i = 0; i < result.size(); i++) {
			assertThat(result.get(i)).as("Invalid media type at " + i).isSameAs(expected.get(i));
		}

	}

	@Test
	void qualityComparator() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audioWave = new MediaType("audio", "wave");
		MediaType audio = new MediaType("audio");
		MediaType audio03 = new MediaType("audio", "*", 0.3);
		MediaType audio07 = new MediaType("audio", "*", 0.7);
		MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
		MediaType textHtml = new MediaType("text", "html");
		MediaType allXml = new MediaType("application", "*+xml");
		MediaType all = MediaType.ALL;

		@SuppressWarnings("removal")
		Comparator<MediaType> comp = MediaType.QUALITY_VALUE_COMPARATOR;

		// equal
		assertThat(comp.compare(audioBasic, audioBasic)).as("Invalid comparison result").isEqualTo(0);
		assertThat(comp.compare(audio, audio)).as("Invalid comparison result").isEqualTo(0);
		assertThat(comp.compare(audio07, audio07)).as("Invalid comparison result").isEqualTo(0);
		assertThat(comp.compare(audio03, audio03)).as("Invalid comparison result").isEqualTo(0);
		assertThat(comp.compare(audioBasicLevel, audioBasicLevel)).as("Invalid comparison result").isEqualTo(0);

		// specific to unspecific
		assertThat(comp.compare(audioBasic, audio)).as("Invalid comparison result").isLessThan(0);
		assertThat(comp.compare(audioBasic, all)).as("Invalid comparison result").isLessThan(0);
		assertThat(comp.compare(audio, all)).as("Invalid comparison result").isLessThan(0);
		assertThat(comp.compare(MediaType.APPLICATION_XHTML_XML, allXml)).as("Invalid comparison result").isLessThan(0);

		// unspecific to specific
		assertThat(comp.compare(audio, audioBasic)).as("Invalid comparison result").isGreaterThan(0);
		assertThat(comp.compare(all, audioBasic)).as("Invalid comparison result").isGreaterThan(0);
		assertThat(comp.compare(all, audio)).as("Invalid comparison result").isGreaterThan(0);
		assertThat(comp.compare(allXml, MediaType.APPLICATION_XHTML_XML)).as("Invalid comparison result")
				.isGreaterThan(0);

		// qualifiers
		assertThat(comp.compare(audio, audio07)).as("Invalid comparison result").isLessThan(0);
		assertThat(comp.compare(audio07, audio)).as("Invalid comparison result").isGreaterThan(0);
		assertThat(comp.compare(audio07, audio03)).as("Invalid comparison result").isLessThan(0);
		assertThat(comp.compare(audio03, audio07)).as("Invalid comparison result").isGreaterThan(0);
		assertThat(comp.compare(audio03, all)).as("Invalid comparison result").isGreaterThan(0);
		assertThat(comp.compare(all, audio03)).as("Invalid comparison result").isLessThan(0);

		// other parameters
		assertThat(comp.compare(audioBasic, audioBasicLevel)).as("Invalid comparison result").isGreaterThan(0);
		assertThat(comp.compare(audioBasicLevel, audioBasic)).as("Invalid comparison result").isLessThan(0);

		// different types
		assertThat(comp.compare(audioBasic, textHtml)).as("Invalid comparison result").isEqualTo(0);
		assertThat(comp.compare(textHtml, audioBasic)).as("Invalid comparison result").isEqualTo(0);

		// different subtypes
		assertThat(comp.compare(audioBasic, audioWave)).as("Invalid comparison result").isEqualTo(0);
		assertThat(comp.compare(audioWave, audioBasic)).as("Invalid comparison result").isEqualTo(0);
	}

	@Test
	@SuppressWarnings("removal")
	public void sortByQualityRelated() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audio = new MediaType("audio");
		MediaType audio03 = new MediaType("audio", "*", 0.3);
		MediaType audio07 = new MediaType("audio", "*", 0.7);
		MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
		MediaType all = MediaType.ALL;

		List<MediaType> expected = new ArrayList<>();
		expected.add(audioBasicLevel);
		expected.add(audioBasic);
		expected.add(audio);
		expected.add(all);
		expected.add(audio07);
		expected.add(audio03);

		List<MediaType> result = new ArrayList<>(expected);
		Random rnd = new Random();
		// shuffle & sort 10 times
		for (int i = 0; i < 10; i++) {
			Collections.shuffle(result, rnd);
			MediaType.sortByQualityValue(result);

			for (int j = 0; j < result.size(); j++) {
				assertThat(result.get(j)).as("Invalid media type at " + j).isSameAs(expected.get(j));
			}
		}
	}

	@Test
	@SuppressWarnings("removal")
	public void sortByQualityUnrelated() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audioWave = new MediaType("audio", "wave");
		MediaType textHtml = new MediaType("text", "html");

		List<MediaType> expected = new ArrayList<>();
		expected.add(textHtml);
		expected.add(audioBasic);
		expected.add(audioWave);

		List<MediaType> result = new ArrayList<>(expected);
		MediaType.sortBySpecificity(result);

		for (int i = 0; i < result.size(); i++) {
			assertThat(result.get(i)).as("Invalid media type at " + i).isSameAs(expected.get(i));
		}
	}

	@Test
	void testWithConversionService() {
		ConversionService conversionService = new DefaultConversionService();
		assertThat(conversionService.canConvert(String.class, MediaType.class)).isTrue();
		MediaType mediaType = MediaType.parseMediaType("application/xml");
		assertThat(conversionService.convert("application/xml", MediaType.class)).isEqualTo(mediaType);
	}

	@Test
	void isConcrete() {
		assertThat(MediaType.TEXT_PLAIN.isConcrete()).as("text/plain not concrete").isTrue();
		assertThat(MediaType.ALL.isConcrete()).as("*/* concrete").isFalse();
		assertThat(new MediaType("text", "*").isConcrete()).as("text/* concrete").isFalse();
	}

	@Test  // gh-26127
	void serialize() throws Exception {
		MediaType original = new MediaType("text", "plain", StandardCharsets.UTF_8);
		MediaType deserialized = SerializationTestUtils.serializeAndDeserialize(original);
		assertThat(deserialized).isEqualTo(original);
		assertThat(original).isEqualTo(deserialized);
	}

	@Test
	void sortBySpecificity() {
		MediaType audioBasic = new MediaType("audio", "basic");
		MediaType audio = new MediaType("audio");
		MediaType audio03 = new MediaType("audio", "*", 0.3);
		MediaType audio07 = new MediaType("audio", "*", 0.7);
		MediaType audioBasicLevel = new MediaType("audio", "basic", Collections.singletonMap("level", "1"));
		MediaType all = MediaType.ALL;

		List<MediaType> expected = new ArrayList<>();
		expected.add(audioBasicLevel);
		expected.add(audioBasic);
		expected.add(audio);
		expected.add(all);
		expected.add(audio07);
		expected.add(audio03);

		List<MediaType> result = new ArrayList<>(expected);
		Random rnd = new Random();
		// shuffle & sort 10 times
		for (int i = 0; i < 10; i++) {
			Collections.shuffle(result, rnd);
			MimeTypeUtils.sortBySpecificity(result);

			assertThat(result).containsExactlyElementsOf(expected);

		}
	}

}
