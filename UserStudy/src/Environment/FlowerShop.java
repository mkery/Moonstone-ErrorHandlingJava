package Environment;

public class FlowerShop {

	public static FlowerInventory getInventory() throws ShopClosedException {
		return new FlowerInventory();
	}

}
