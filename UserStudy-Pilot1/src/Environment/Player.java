package Environment;

public class Player {

	/*
	 * player.loadAvatar();
			player.loadVoice();
			player.updateItems();
			player.addWeapons();
			player.addHorse();
			player.walkCycle();
			player.introCycle();
	 */
	
	public void loadAvatar() throws LoadException
	{
		throw new LoadException();
	}
	
	public void updateItems() throws ItemsException
	{
		throw new ItemsException();
	}
	
	public void addWeapons() throws ItemsException
	{
		updateItems();
	}
	
	public void addHorse() throws ItemsException
	{
		throw new ItemsException();
	}
	
	public void walkCycle() throws PlayerException
	{
		int i = (int) (Math.random() * 11.0);
		if(i > 5)
			throw new PlayerException();
		else
			throw new LoadException();
	}
	
	public void introCycle() throws PlayerException
	{
		walkCycle();
	}
	
	public class PlayerException extends Exception{
		
	}
	public class LoadException extends PlayerException{
		
	}
	public class ItemsException extends PlayerException{
		
	}
	public void loadVoice() throws LoadException {
		throw new LoadException();
		
	}
}
