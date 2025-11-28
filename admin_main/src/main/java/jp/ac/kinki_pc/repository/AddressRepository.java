// jp/ac/kinki_pc/repository/AddressRepository.java
package jp.ac.kinki_pc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {
	
}