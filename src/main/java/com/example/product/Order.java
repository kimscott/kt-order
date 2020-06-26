package com.example.product;

import java.util.Optional;

import javax.persistence.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Entity
@Table(name="Order_table")
public class Order {

	@Id
	@GeneratedValue
	Long orderId;
	Long productId;
	int qty;
	String productName;
	String orderStatus = "OrderPlaced";
	
	
//	@PrePersist
//	public void productCheck() {
//		
//		RestTemplate restTemplate = OrderApplication.applicationContext.getBean(RestTemplate.class);
//        Environment env = OrderApplication.applicationContext.getEnvironment();
//        
//        
//		String productUrl = env.getProperty("api.url.product") + "/product/" + productId;
//
//        ResponseEntity<String> productEntity = restTemplate.getForEntity(productUrl, String.class);
//        JsonParser parser = new JsonParser();
//        JsonObject jsonObject = parser.parse(productEntity.getBody()).getAsJsonObject();
//
//
//        if( jsonObject.get("stock").getAsInt() < this.getQty()){
//            throw new RuntimeException("No Available stock!");
//        }
//		
//	}
	
	@PostPersist
	public void sendOrderEvent() {
		OrderPlaced orderPlaced = new OrderPlaced();
	    orderPlaced.setOrderId(this.getOrderId());
	    orderPlaced.setProductId(this.getProductId());
	    orderPlaced.setQty(this.getQty());
	    orderPlaced.setProductName(this.getProductName());
	    

	    ObjectMapper objectMapper = new ObjectMapper();
	    String json = null;

	    try {
	        json = objectMapper.writeValueAsString(orderPlaced);
	    } catch (JsonProcessingException e) {
	        throw new RuntimeException("JSON format exception", e);
	    }

	    Processor processor = OrderApplication.applicationContext.getBean(Processor.class);
	    MessageChannel outputChannel = processor.output();

	    outputChannel.send(MessageBuilder
	            .withPayload(json)
	            .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
	            .build());
		
		
	}
	
	// put, patch 
	@PostUpdate
	public void cancelOrder() {
		
		if(orderStatus != null && orderStatus.equals("cancel")) {
		
			// 주문 번호에 의하여 세부 내용 조회
			OrderRepository orderRepository = 
					OrderApplication.applicationContext.getBean(OrderRepository.class);
			
			Optional<Order> opt = orderRepository.findById(this.getOrderId());
			Order orders = opt.get();
			// or
			
			orderRepository.findById(this.getOrderId()).ifPresent(
					order -> {
						
						OrderCancelled orderCancelled = new OrderCancelled();
						orderCancelled.setOrderId(order.getOrderId());
						orderCancelled.setProductId(order.getProductId());
						orderCancelled.setQty(order.getQty());
					    orderCancelled.setProductName(order.getProductName());
				
					    ObjectMapper objectMapper = new ObjectMapper();
					    String json = null;
				
					    try {
					        json = objectMapper.writeValueAsString(orderCancelled);
					    } catch (JsonProcessingException e) {
					        throw new RuntimeException("JSON format exception", e);
					    }
				
					    Processor processor = OrderApplication.applicationContext.getBean(Processor.class);
					    MessageChannel outputChannel = processor.output();
				
					    outputChannel.send(MessageBuilder
					            .withPayload(json)
					            .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
					            .build());
						
					}
					
					);
			
			
		}
	}
	

	
	
	
	
	
	
	
	
	
	
	
	public String getOrderStatus() {
		return orderStatus;
	}


	public void setOrderStatus(String orderStatus) {
		this.orderStatus = orderStatus;
	}


	public Long getOrderId() {
		return orderId;
	}
	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}
	public Long getProductId() {
		return productId;
	}
	public void setProductId(Long productId) {
		this.productId = productId;
	}
	public int getQty() {
		return qty;
	}
	public void setQty(int qty) {
		this.qty = qty;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	
	
}
