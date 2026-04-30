ALTER TABLE edge_device
  ADD CONSTRAINT uk_edge_device_vehicle_id UNIQUE (vehicle_id);
