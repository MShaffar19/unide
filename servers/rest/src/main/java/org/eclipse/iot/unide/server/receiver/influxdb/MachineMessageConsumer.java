/*
 * Copyright (c) 2017 Bosch Software Innovations GmbH. All rights reserved.
 */

package org.eclipse.iot.unide.server.receiver.influxdb;

import java.util.concurrent.TimeUnit;

import org.eclipse.iot.unide.ppmp.commons.Device;
import org.eclipse.iot.unide.ppmp.messages.Message;
import org.eclipse.iot.unide.ppmp.messages.MessagesWrapper;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;

/**
 * Consumer class for Message
 */
class MachineMessageConsumer extends AbstractInfluxDbConsumer<MessagesWrapper> {

	private static final String MEASUREMENT_NAME = "ppmp_messages";

	MachineMessageConsumer(InfluxDB connection, String databaseName) {
		super(connection, databaseName);
	}

	/**
	 * Inserts a single Message-Array with device information
	 *
	 * @param message
	 *            Message-Array as PPMP java binding object
	 * @param device
	 *            Device as PPMP java binding object
	 */
	private void insert(Message message, Device device) {
		BatchPoints batchPoints = BatchPoints.database(getDatabaseName()).consistency(InfluxDB.ConsistencyLevel.ALL)
				.build();
		Builder builder = Point.measurement(MEASUREMENT_NAME).time(message.getTimestamp().toInstant().toEpochMilli(),
				TimeUnit.MILLISECONDS);

		setTags(builder, message, device);
		setFields(builder, message);

		Point point = builder.build();
		batchPoints.point(point);
		getInfluxDb().write(batchPoints);
	}

	/**
	 * Sets the tag / index information in the db
	 *
	 * @param builder
	 *            - a pointer to the database
	 * @param message
	 *            the data of a single message
	 * @param device
	 *            the device object of the payload
	 */
	private void setTags(Builder builder, Message message, Device device) {
		builder.tag("deviceId", device.getDeviceID());
		builder.tag("code", message.getCode());
	}

	/**
	 * Sets other, non-indexed information in the db
	 *
	 * @param builder
	 *            - a pointer to the database
	 * @param message
	 *            the data of a single message
	 */
	private void setFields(Builder builder, Message message) {
		if (isNotNull(message.getOrigin())) {
			builder.addField("origin", message.getOrigin());
		}

		if (isNotNull(message.getSeverity())) {
			builder.addField("severity", message.getSeverity().name());
		}

		if (isNotNull(message.getTitle())) {
			builder.addField("title", message.getTitle());
		}

		if (isNotNull(message.getDescription())) {
			builder.addField("description", message.getDescription());
		}

		if (isNotNull(message.getHint())) {
			builder.addField("hint", message.getHint());
		}

		if (isNotNull(message.getType())) {
			builder.addField("type", message.getType().name());
		}
	}

	@Override
	public void accept(MessagesWrapper data) {
		Device device = data.getDevice();
		data.getMessages().forEach(message -> insert(message, device));
	}
}
