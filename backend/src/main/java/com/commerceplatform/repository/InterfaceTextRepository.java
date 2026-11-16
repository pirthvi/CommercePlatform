package com.commerceplatform.repository;

import com.commerceplatform.domain.InterfaceText;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InterfaceTextRepository extends JpaRepository<InterfaceText, String> {
    List<InterfaceText> findByIdStartingWithOrderById(String prefix);
}
