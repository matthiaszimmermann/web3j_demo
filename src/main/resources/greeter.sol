pragma solidity ^0.4.6;

contract greeter {

    /* Owner of this contract */
    address owner;
    
    /* Counter for deposits calls */
    uint public deposits;
    
    /* Configurable greeting */
    string greeting;

    /* Constructor runs when contract is deployed */
    function greeter(string _greeting) public {
        owner = msg.sender;
        greeting = _greeting;
        deposits = 0;
    }

    /* 
     * Default function. 
     * 'payable': Allows to move funds to contract.
     * Changes state: Costs gas and needs contract transaction.
     */
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