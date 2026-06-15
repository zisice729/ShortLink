
from kafka import KafkaConsumer
import json

consumer = KafkaConsumer(
    'short_link_log',
    bootstrap_servers=['localhost:9092'],
    auto_offset_reset='earliest',
    enable_auto_commit=True,
    group_id='short_link_consumer'
)

pv_counter = {}
uv_counter = {}

for msg in consumer:
    try:
        log_json = json.loads(msg.value.decode("utf-8"))
        short_code = log_json.get('code', '')
        ip = log_json.get('ip', '')
        
        if short_code:
            pv_counter[short_code] = pv_counter.get(short_code, 0) + 1
            
            if ip:
                if short_code not in uv_counter:
                    uv_counter[short_code] = set()
                uv_counter[short_code].add(ip)
        
        if short_code:
            print(f"PV[{short_code}]: {pv_counter[short_code]}, UV: {len(uv_counter.get(short_code, set()))}")
            
    except Exception as e:
        print(f"Error processing message: {e}")
