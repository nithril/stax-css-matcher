package org.nlab.xml.stream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.Validate;
import org.jooq.lambda.fi.util.function.CheckedSupplier;
import org.nlab.exception.UncheckedExecutionException;
import org.nlab.util.IoCloser;
import org.nlab.xml.stream.consumer.XmlConsumer;
import org.nlab.xml.stream.context.StreamContext;
import org.nlab.xml.stream.factory.StaxCachedFactory;
import org.nlab.xml.stream.reader.PartialXmlStreamReaderSpliterator;
import org.nlab.xml.stream.reader.XmlMatcherStreamReader;
import org.nlab.xml.stream.reader.XmlStreamReaderSpliterator;

/**
 * Created by nlabrot on 14/12/15.
 */
public class XmlStreamSpec {

	private final Path path;
	private final InputStream inputStream;
	private final XMLStreamReader reader;
	private boolean closeOnFinish = false;
	private boolean partial = false;

	public XmlStreamSpec(Path path) {
		this.path = path;
		this.inputStream = null;
		this.reader = null;
	}

	public XmlStreamSpec(InputStream inputStream) {
		this.path = null;
		this.inputStream = inputStream;
		this.reader = null;
	}

	public XmlStreamSpec(XMLStreamReader reader) {
		this.path = null;
		this.inputStream = null;
		this.reader = reader;
	}

	public XmlStreamSpec hint() {
		return this;
	}

	public XmlStreamSpec closeOnFinish() {
		this.closeOnFinish = true;
		return this;
	}

	public XmlStreamSpec partial() {
		this.partial = true;
		return this;
	}

	public XmlStream stream() throws IOException, XMLStreamException {

		if (path != null) {
			return createStream(path);
		} else if (inputStream != null) {
			return createStream(inputStream, closeOnFinish);
		} else if (reader != null) {
			return createStream(reader, closeOnFinish);
		}

		return null;
	}

	public CheckedSupplier<XmlStream> sstream() throws IOException, XMLStreamException {
		return () -> stream();
	}


	public XmlConsumer consumer() throws IOException, XMLStreamException {
		return new XmlConsumer(stream());
	}

	public XmlConsumer uncheckedConsumer() {
		try {
			return new XmlConsumer(stream());
		} catch (Exception e) {
			throw new UncheckedExecutionException(e);
		}
	}


	public static XmlStreamSpec with(String path) {
		return new XmlStreamSpec(Paths.get(path));
	}

	public static XmlStreamSpec with(Path path) {
		return new XmlStreamSpec(path);
	}

	public static XmlStreamSpec with(InputStream inputStream) {
		return new XmlStreamSpec(inputStream);
	}

	public static XmlStreamSpec with(XMLStreamReader reader) {
		return new XmlStreamSpec(reader).partial();
	}

	public static XmlStreamSpec with(StreamContext streamContext) {
		return new XmlStreamSpec(streamContext.getStreamReader()).partial();
	}


	private XmlStream createStream(Path file) throws XMLStreamException, IOException {
		InputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(Files.newInputStream(file));
			return createStream(inputStream, true);
		} catch (Exception e) {
			IoCloser.ioCloser().close(inputStream);
			throw e;
		}
	}

	private XmlStream createStream(XMLStreamReader reader, boolean close) {
		try {
			XmlMatcherStreamReader xmlMatcherStreamReader = createOrCastMatcherStreamReader(reader);
			Stream<StreamContext> stream = StreamSupport.stream(createXmlStreamReaderSpliterator(xmlMatcherStreamReader), false);

			if (close) {
				stream = stream.onClose(() -> IoCloser.ioCloser().close(reader));
			}

			return new XmlStream(stream, xmlMatcherStreamReader);

		} catch (Exception e) {
			if (close) {
				IoCloser.ioCloser().close(reader);
			}
			throw e;
		}
	}

	private XmlStream createStream(InputStream is, boolean close) throws XMLStreamException {
		Validate.notNull(is);

		XMLStreamReader streamReader = null;
		try {
			streamReader = StaxCachedFactory.getInputFactory().createXMLStreamReader(is);

			XmlMatcherStreamReader xmlMatcherStreamReader = createOrCastMatcherStreamReader(streamReader);
			Stream<StreamContext> stream = StreamSupport.stream(createXmlStreamReaderSpliterator(xmlMatcherStreamReader), false);

			if (close) {
				stream.onClose(IoCloser.promiseIoCloser(streamReader, is));
			} else {
				stream.onClose(IoCloser.promiseIoCloser(streamReader));
			}

			return new XmlStream(stream, xmlMatcherStreamReader);

		} catch (Exception e) {
			IoCloser ioCloser = IoCloser.ioCloser().close(streamReader);
			if (close) {
				ioCloser.close(is);
			}
			throw e;
		}
	}

	private XmlMatcherStreamReader createOrCastMatcherStreamReader(XMLStreamReader reader) {
		if (reader instanceof XmlMatcherStreamReader){
			return (XmlMatcherStreamReader) reader;
		}else{
			return new XmlMatcherStreamReader(reader);
		}
	}

	private Spliterator<StreamContext> createXmlStreamReaderSpliterator(XmlMatcherStreamReader reader){
		if (partial){
			return new PartialXmlStreamReaderSpliterator(reader);
		}else{
			return new XmlStreamReaderSpliterator(reader);
		}
	}



}
