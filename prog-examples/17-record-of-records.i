type Address is record
    var city: string
    var zip: integer
end

type Person is record
    var name: string
    var home: Address
end

routine buildAndShow() is
    var p: Person
    p.name := "John"
    p.home.city := "Innopolis"
    p.home.zip := 420500
    print p.name + ", " + p.home.city
end
