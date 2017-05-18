pragma solidity ^0.4.6;

contract greeter {

    /* Define variable owner of the type address */
    address owner;
    
    /* Define public variable that counts the number of deposits calls */
    uint public deposits;
    
    /* Define variable greeting of the type string */
    string greeting;

    /* This runs when the contract is executed */
    function greeter(string _greeting) public {
        owner = msg.sender;
        greeting = _greeting;
        deposits = 0;
    }

    /* Default function to accept payments. Counts # of deposits */
    function() payable { 
        deposits += 1;
    }

    /* Main function */
    function greet() constant returns (string) {
        return greeting;
    }

    /* Function to recover the funds on the contract */
    function kill() { 
        if (msg.sender == owner)
            selfdestruct(owner); 
    }
}