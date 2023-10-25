terraform {
  backend "azurerm" {}
   required_providers {
      azurerm = {
        source  = "hashicorp/azurerm"
        version = "~> 3.40"
      }

     azuread = {
       source  = "hashicorp/azuread"
       version = "2.44.1"
     }
    }
}
